guacamole-auth-json
===================

guacamole-auth-json is an authentication extension for [Apache
Guacamole](http://guacamole.apache.org/) which authenticates users using JSON
which has been signed using **HMAC/SHA-256** and encrypted with **128-bit AES
in CBC mode**. This JSON contains all information describing the user being
authenticated, as well as any connections they have access to.

Configuring Guacamole to accept encrypted JSON
----------------------------------------------

To verify and decrypt the received signed and encrypted JSON, a secret key must
be generated which will be shared by both the Guacamole server and systems that
will generate the JSON data. As guacamole-auth-json uses 128-bit AES, this key
must be 128 bits.

An easy way of generating such a key is to echo a passphrase through the
"md5sum" utility. This is the technique OpenSSL itself uses to generate 128-bit
keys from passphrases. For example:

    $ echo -n "ThisIsATest" | md5sum
    4c0b569e4c96df157eee1b65dd0e4d41  -

The generated key must then be saved within `guacamole.properties` as the full
32-digit hex value using the `json-secret-key` property:

    json-secret-key: 4c0b569e4c96df157eee1b65dd0e4d41

JSON format
-----------

The general format of the JSON (prior to being encrypted, signed, and sent to
Guacamole), is as follows:

    {

        "username" : "arbitraryUsername",
        "expires" : TIMESTAMP,
        "connections" : {

            "Connection Name" : {
                "protocol" : "PROTOCOL",
                "parameters" : {
                    "name1" : "value1",
                    "name2" : "value2",
                    ...
                }
            },

            ...

        }

    }

where `TIMESTAMP` is a standard UNIX epoch timestamp with millisecond
resolution (the number of milliseconds since midnight of January 1, 1970 UTC)
and `PROTOCOL` is the internal name of any of Guacamole's supported protocols,
such as `vnc`, `rdp`, or `ssh`.

The JSON will cease to be accepted as valid after the server time passes the
timestamp. If no timestamp is specified, the data will not expire.

The top-level JSON object which must be submitted to Guacamole has the
following properties:

Property name | Type     | Description
--------------|----------|------------
`username`    | `string` | The unique username of the user authenticated by the JSON. If the user is anonymous, this should be the empty string (`""`).
`expires`     | `number` | The absolute time after which the JSON should no longer be accepted, even if the signature is valid, as a standard UNIX epoch timestamp with millisecond resolution (the number of milliseconds since midnight of January 1, 1970 UTC).
`connections` | `object` | The set of connections which should be exposed to the user by their corresponding, unique names. If no connections will be exposed to the user, this can simply be an empty object (`{}`).

Each normal connection defined within each submitted JSON object has the
following properties:

Property name | Type     | Description
--------------|----------|------------
`id`          | `string` | An optional opaque value which uniquely identifies this connection across all other connections which may be active at any given time. This property is only required if you wish to allow the connection to be shared or shadowed.
`protocol`    | `string` | The internal name of a supported protocol, such as `vnc`, `rdp`, or `ssh`.
`parameters`  | `object` | An object representing the connection parameter name/value pairs to apply to the connection, as documented in the [Guacamole manual](https://guacamole.apache.org/doc/gug/configuring-guacamole.html#connection-configuration).

Connections which share or shadow other connections use a `join` property
instead of a `protocol` property, where `join` contains the value of the `id`
property of the connection being joined:

Property name | Type     | Description
--------------|----------|------------
`id`          | `string` | An optional opaque value which uniquely identifies this connection across all other connections which may be active at any given time. This property is only required if you wish to allow the connection to be shared or shadowed. (Yes, a connection which shadows another connection may itself be shadowed.)
`join`        | `string` | The opaque ID given within the `id` property of the connection being joined (shared / shadowed).
`parameters`  | `object` | An object representing the connection parameter name/value pairs to apply to the connection, as documented in the [Guacamole manual](https://guacamole.apache.org/doc/gug/configuring-guacamole.html#connection-configuration). Most of the connection configuration is inherited from the connection being joined. In general, the only property relevant to joining connections is `read-only`.

If a connection is configured to join another connection, that connection will
only be usable if the connection being joined is currently active. If two
connections are established having the same `id` value, only the last
connection will be joinable using the given `id`.

Generating encrypted JSON
-------------------------

To authenticate a user with the above JSON format, the JSON must be both signed
and encrypted using the same 128-bit secret key specified with the
`json-secret-key` within `guacamole.properties`:

1. Generate JSON in the format described above
2. Sign the JSON using the secret key (the same 128-bit key stored within
   `guacamole.properties` with the `json-secret-key` property) with
   **HMAC/SHA-256**. Prepend the binary result of the signing process to the
   plaintext JSON that was signed.
3. Encrypt the result of (2) above using **AES in CBC mode**, with the initial
   vector (IV) set to all zero bytes.
4. Encode the encrypted result using base64.
5. POST the encrypted result to the `/api/tokens` REST endpoint as the value of
   an HTTP parameter named `data` (or include it in the URL of any Guacamole
   page as a query parameter named `data`).

   For example, if Guacamole is running on localhost at `/guacamole`, and
   `BASE64_RESULT` is the result of the above process, the equivalent run of
   the "curl" utility would be:

       $ curl --data-urlencode "data=BASE64_RESULT" http://localhost:8080/guacamole/api/tokens

   **NOTE:** Be sure to URL-encode the base64-encoded result prior to POSTing
   it to `/api/tokens` or including it in the URL. Base64 can contain both "+"
   and "=" characters, which have special meaning within URLs.

If the data is invalid in any way, if the signature does not match, if
decryption or signature verification fails, or if the submitted data has
expired, the REST service will return an invalid credentials error and fail
without user-visible explanation. Details describing the error that occurred
will be in the Tomcat logs, however.

Reference implementation
------------------------

The source includes a shell script, `doc/encrypt-json.sh`, which uses the
OpenSSL command-line utility to encrypt and sign JSON in the manner that
guacamole-auth-json requires. It is thoroughly commented and should work well
as a reference implementation, for testing, and as a point of comparison for
development. The script is run as:

    $ ./encrypt-json.sh HEX_ENCRYPTION_KEY file-to-sign-and-encrypt.json

For example, if you have a file called `auth.json` containing the following:

    {
        "username" : "test",
        "expires" : "1446323765000",
        "connections" : {
            "My Connection" : {
                "protocol" : "rdp",
                "parameters" : {
                    "hostname" : "10.10.209.63",
                    "port" : "3389"
                }
            },
            "My OTHER Connection" : {
                "protocol" : "rdp",
                "parameters" : {
                    "hostname" : "10.10.209.64",
                    "port" : "3389"
                }
            }
        }
    }

and you run:

    $ ./encrypt-json.sh 4C0B569E4C96DF157EEE1B65DD0E4D41 auth.json

You will receive the following output:

    le2Ug6YIo4perD2GV17QtWvOdfSemVDDtCOdRYJlbdUf3fhN+63LpQa1RDkzU7Zc
    DW3+OtyTCBGQ7OLO+HpG6pHNom76BXpmnHSRx1UdQ3WVZelPUXEDzxe74aN6DUP9
    G9isXhBMdLUhZwEJf4k4Gpzt9MHAH5PufSKq3DO1UHnrRjdGbKKddug2BcuDrwJM
    UJf1tRX9CAEC11/gWEwrHDOhH/abeyeDyElbaEG/oOY8EdoFNYgUsjI2x31OpCuB
    sEv7FOFafL05wEoIFv0/pPft0DHk7GuvHBBCqXuK98yMEo3d0zD5D+IsOY8Rmm1+
    0CoWkX22mqyRQMFS2fTp/fClCN4QLb0aNn+unweTimd2SXN9cjREmZknXf7Tj8oU
    /FNXc37i0HEfG5aVgp5znMCwwRAOFnFhLqG3K2yaTRE+hLNBxltIjLfFmNG5TZZA
    gUdKyuegsOd0KS5iHdW6tPI01AwfRO9y2z20t3flsgDp50EGWjT2/TTA5Nkjnnjk
    JXNzCOfM7DCI/ioEz6Ga140qXfOX/g8SGiukpwt+j0ANI573TdVt7nsp7MZX2qKg
    2GcoNqjBqQxqpqI5ZYz4KVfD4cYu8KDZ9MiFMzbUwwKNSzYxiep1KJwiG0HQThHg
    oX2FJYOFCFcinQgGkUOaBJK1K0bo1ouaBSe4iGPjd54=

The resulting base64 data above, if submitted using the `data` parameter to
Guacamole, will authenticate a user and grant them access to the connections
described in the JSON (at least until the expires timestamp is reached, at
which point the JSON will no longer be accepted).

