#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

#
# generate-license-files.sh
# -------------------------
#
# Automatically iterates through the provided runtime dependencies of the
# project in the current directory, creating new LICENSE and NOTICE files which
# contain all the license information of bundled dependencies, as well as any
# required notices.
#
# This script is expected to be invoked as part of the build process of the
# various guacamole-client subprojects.
#
# USAGE:
#
#     path/to/generate-license-files.sh DEPENDENCY_LIST OUTPUT_DIRECTORY
#
# where DEPENDENCY_LIST is the list of dependencies to consider when generating
# LICENSE and NOTICE (as produced by "mvn dependency:list" or our
# DependencyListPlugin for Webpack) and OUTPUT_DIRECTORY is the directory in
# which the LICENSE and NOTICE files should be written.
#
# If DEPENDENCY_LIST is a directory, all normal files within the directory will
# be concatenated to produce the list.
#
# Ignoring license errors
# -----------------------
#
# By default, this script will exit with a non-zero exit code if any license
# errors are encountered, failing the build. If this is undesirable (dependency
# changes are being tested, a full list of all license errors across all
# projects is needed, etc.), set the IGNORE_LICENSE_ERRORS environment variable
# to "true".
#
# Structure of license information
# --------------------------------
#
# The LICENSE and NOTICE files of the guacamole-client project are used as the
# basis for the generated LICENSE and NOTICE files. It is expected that the
# guacamole-client LICENSE will may contain license information for
# subcomponents that had to be copied verbatim into the source tree, with that
# information following a human-readable preamble that begins with a solid line
# of "=" characters. This line of "=" characters MUST BE INCLUDED when such
# dependencies are present, as it is specifically searched for by this script
# when determining what parts of the main LICENSE file should be copied over.
#
# License information for the bundled runtime dependencies of all subprojects
# is included in the subdirectories of the "doc/licenses/" directory of the
# main guacamole-client source tree. Each subdirectory is associated with a
# single, logical dependency, with the coordinates of that dependency listed in
# the "dep-coordinates.txt" file within that subdirectory. There may be
# multiple coordinates associated with a dependency, in which case each
# relevant set of coordinates should be listed on its own line.
#
# For dependencies that are not associated with coordinates that can be
# automatically deteremined, the licenses of those dependencies should instead
# be documented within subdirectories of the "src/licenses/bundled/" directory
# of the relevant subproject.
#
# Regardless of whether a dependency is documented within the top-level
# guacamole-client project or within the relevant subproject, the subdirectory
# for each dependency must contain at least one file containing headers describing
# the dependency, the copyright holder(s), and the license.
#
# Filenames that contain the word "notice", regardless of case, are considered
# by this script to be the notice file mentioned within the Apache License, and
# will be copied verbatim into the generated NOTICE file.
#
# Files that that are not recognized as any other type of file are considered
# by this script to be the license and will be included verbatim within the
# generated LICENSE if they are less than 50 lines.
#
# ** PLEASE SEE THE EXISTING DEPENDENCY LICENSE DIRECTORIES FOR EXAMPLES OF
# THIS IN PRACTICE **
#
# License header fields
# ---------------------
#
# The possible header fields are:
#
# Field   | Description
# ------- | -------------------------------------------------------------------
# Name    | REQUIRED. The name of the component.
# License | REQUIRED. The human-readable name of the copyright license (Apache v2.0, MIT, etc.).
# From    | REQUIRED. The original developer, current maintainer, or copyright holder.
# URL     | REQUIRED. The URL of the component's web page.
# Version | The version number of the component (if not determined by Maven/NPM).
# Source  | The URL that the current file may be downloaded from.
# +       | Arbitrary comments or information that should be included with the license.
#
# The value for the Source header may contain any of the following variables in
# shell format ($VARIABLE or ${VARIABLE}), to allow for URLs that vary by version:
#
# Variable  | Description
# --------- | ----------------------------------------------------------------
# VERSION   | The exact version number provided.
# _VERSION_ | The version number provided, with all periods replaced with underscore.
# MAJOR     | The major (leftmost) component of the version number.
# MINOR     | The minor (second from the left) component of the version number.
# PATCH     | The patch (third from the left) component of the version number.
# REV       | The "revision" (fourth from the left) component of the version number.
#
# If a field is included multiple times within the same header, each value is
# considered to be one line of that field. If multiple "Source" headers are
# provided, each is tried in order until the download succeeds.
#
# ** IMPORTANT: ** Except for "Source", which may be included in any license
# information file to permit dynamic updates of that information, all of these
# fields MUST be located within the same file.
#

##
## The filename of the file containing whether any errors have been logged. The
## contents of this file will be set to "1" if at least one error occurred. A
## file is used here instead of a simple environment variable to allow error()
## to be called from within pipelines and still be detectable.
##
HAS_ERRORS="`mktemp`"
trap 'rm -f "$HAS_ERRORS"' EXIT

##
## The maximum size of a license that may be included verbatim within the
## generated LICENSE file. Larger licenses will only be included separately.
##
LICENSE_COPY_LIMIT=50

##
## The directory containing this script and all license information. This
## should be "doc/licenses/" within the guacamole-client source tree.
##
LICENSES_DIR="`dirname "$0"`"

##
## The base directory of the guacamole-client source tree.
##
BASEDIR="$LICENSES_DIR/../.."

##
## The file containing all relevant runtime dependencies, as generated by
## "mvn dependency:list".
##
DEPENDENCY_LIST="$1"

##
## The output directory in which the generated LICENSE, NOTICE, etc. files
## should be placed.
##
OUTPUT_DIR="$2"

##
## Lists the license information directories (subdirectories of the
## "doc/licenses/" directory in the main guacamole-client source tree) that
## apply to the list of runtime dependencies provided via STDIN. If any runtime
## dependencies are not described by license information included in the
## guacamole-client source tree, a error is printed to STDERR.
##
## The license information directories for dependencies that are not pulled in
## automatically (subdirectories of the "src/licenses/bundled/" directory
## within the current project) will also be included, if any.
##
list_dependency_license_info() {

    # List the license directories of all runtime dependencies, as dictated by
    # the "dep-coordinates.txt" files included within those directories
    sed 's/^[[:space:]]\+//' | grep -o '^[^: ]\+\(:[^: ]*\)\{1,3\}' \
        | while read DEPENDENCY; do

        local DEPENDENCY_REGEX="`echo "$DEPENDENCY" | sed 's/[^:]*/\\\\(&\\\\|\*\\\\)/g'`"
        local VERSION="`echo "$DEPENDENCY" | sed 's/.*://'`"

        if ! grep -l "^$DEPENDENCY_REGEX[[:space:]]*$" "$LICENSES_DIR"/*/dep-coordinates.txt \
            | sed "s/$/ $VERSION/g"; then
            error "License information missing for $DEPENDENCY"
        fi

    done | sort -u -k 1,1 | while read LICENSE_INFO_COORDS_FILE VERSION; do
        printf '%s %s\n' "`dirname "$LICENSE_INFO_COORDS_FILE"`" "$VERSION"
    done

    # Include license directories for all dependencies not pulled in automatically
    if [ -d ./src/licenses/bundled/ ]; then
        find src/licenses/bundled/ -mindepth 1 -maxdepth 1 -type d
    fi

}

##
## Sorts the given list of license information directories by the name of the
## dependency, as determined by the header line within that dependency's
## README. The list of directories is read from STDIN, and the sorted result is
## written to STDOUT. Each directory must be on its own line.
##
sort_dependency_license_info() {
    while read LICENSE_INFO VERSION; do

        local PRIMARY_FILE="`primary_file "$LICENSE_INFO"`"

        # Skip if primary file missing (error will be logged by primary_file
        # function call above)
        [ -n "$PRIMARY_FILE" ] || continue

        local NAME="`get_header_value "$PRIMARY_FILE" Name`"
        printf "%s\t%s %s\n" "$NAME" "$LICENSE_INFO" "$VERSION"

    done | sort -f | cut -f2
}

##
## Prints the given informational message to STDERR.
##
## @param ...
##     The message to print.
##
info() {
    echo "$@" >&2
}

##
## Prints the given error message to STDERR, updating HAS_ERRORS
## appropriately.
##
## @param ...
##     The message to print.
##
error() {
    echo "ERROR: $@" >&2
    echo "1" > "$HAS_ERRORS"
}

##
## Prints the first line from STDIN. If more than one line is provided,
## those lines are omitted, and an error exit code is returned.
##
## @return
##     Zero (success) if no more than one line was provided on STDIN,
##     non-zero (failure) otherwise.
##
single_result() {
    read RESULT && echo "$RESULT"
    ! read DUMMY
}

##
## Searches the given directory for a license file, prints the path to the
## license file found. If multiple files are found, only the first file is
## printed, and an error is logged to STDERR.
##
## @param DIR
##     The directory to search.
##
license_file() {
    DIR="$1"
    find "$DIR" -mindepth 1 \
            -a \! -iname "*notice*" \
            -a \! -name "dep-coordinates.txt" \
        | if ! single_result; then
           error "Multiple license files found within $DIR"
        fi 
}

##
## Searches the given directory for a notice file, prints the path to the
## notice file found. If multiple files are found, only the first file is
## printed, and a error is logged to STDERR.
##
## @param DIR
##     The directory to search.
##
notice_file() {
    DIR="$1"
    find "$DIR" -iname "*notice*" \
        | if ! single_result; then
           error "Multiple notice files found within $DIR"
        fi 
}

##
## Determines which file within the given directory is the primary source of
## license information, dictated by whichever file contains the "License"
## field. If such a file exists, it is printed to STDOUT. If not, an error is
## logged.
##
## @param DIR
##     The directory to search.
##
primary_file() {

    DIR="$1"
    for FILENAME in "$DIR"/*; do
        if [ ! -d "$FILENAME" ] && [ -n "`get_header_value "$FILENAME" License`" ]; then
            echo "$FILENAME"
            return
        fi
    done

    error "No file containing \"License\" field found within $DIR"

}

##
## Prints the contents of the given file, excluding any blank lines at the
## beginning and end of the file.
##
## @param FILENAME
##     The file to print.
##
trim_file() {

    FILENAME="$1"

    # Find line number of first and last non-blank lines
    FIRST_LINE="`awk '/[^\t ]/ {print NR; exit}' "$FILENAME"`"
    LAST_LINE="`awk '/[^\t ]/ {last=NR} END {print last}' "$FILENAME"`"

    # Print the contents of the file between those lines, inclusive
    awk "NR==$FIRST_LINE,NR==$LAST_LINE" "$FILENAME"

}

##
## Reads text from STDIN, printing that text to STDOUT with leading spaces. If
## the text consists of only a single line, a single leading space is added. If
## the text consists of multiple lines, each line is indented by two levels,
## with four spaces per level.
##
format_multiline() {

    local INDENT="        "
    local NEXT_LINE

    read FIRST_LINE

    if read NEXT_LINE; then
        printf '\n'
        printf "%s%s\n" "$INDENT" "$FIRST_LINE"
        printf "%s%s\n" "$INDENT" "$NEXT_LINE"
        while read NEXT_LINE; do
            printf "%s%s\n" "$INDENT" "$NEXT_LINE"
        done
    else
        printf ' %s\n' "$FIRST_LINE"
    fi
    
}

##
## Performs variable substitution on STDIN, replacing version-related variables
## with values derived from the given version number. Variables must be
## formatted as "$VARIABLE" or "${VARIABLE}".
##
## Supported variables are:
##
## @param VERSION
##     The version number to substitute.
##
subst_version() {

    local VERSION="$1"
    local _VERSION_="`echo "$VERSION" | sed 's/\./_/g'`"

    local MAJOR MINOR PATCH REV
    read -r MAJOR MINOR PATCH REV <<EOF
`echo "$VERSION" | sed 's/[^0-9]\+/ /g'`
EOF

    env "VERSION=$VERSION" \
        "_VERSION_=$_VERSION_" \
        "MAJOR=$MAJOR" \
        "MINOR=$MINOR" \
        "PATCH=$PATCH" \
        "REV=$REV" \
        envsubst '$VERSION $_VERSION_ $MAJOR $MINOR $PATCH $REV'

}

##
## Retrieves all header lines from the given file. The header lines of a
## license-related file are all lines that precede the first "BEGIN LICENSE
## FILE" marker.
##
## @param FILENAME
##     The file to retrieve the header from.
##
get_header() {

    local FILENAME="$1"

    sed -n \
        -e '/^# --- BEGIN LICENSE FILE \(\[[^]]*\] \)\?---$/q' \
        -e 's/^#[[:space:]]*//p' \
        "$FILENAME" | grep .

}

##
## Retrieves the header field having the given name from the given file. The
## header lines of a license-related file are all lines that precede the first
## "BEGIN LICENSE FILE" marker, and a header field is a name/value pair
## included within this header on its own line in the form "NAME: VALUE", where
## "NAME" is the field name and "VALUE" is the desired value.
##
## If a field is included multiple times within the same header, each value is
## considered to be one line of that field.
##
get_header_value() {

    local FILENAME="$1"
    local FIELD="$2"

    get_header "$FILENAME" | grep "^$FIELD:" \
        | sed 's/^[^:]*:[[:space:]]*//' \
        | sed 's/[[:space:]]\+$//' \
        | grep .

}

##
## Retrieves the full license contents from the given license file. If the
## license information is version-specific, the provided version is used to
## determine which contents are returned based the versions noted in "BEGIN
## LICENSE FILE [VERSION]" and "END LICENSE FILE [VERSION]" markers. If no
## version is provided, only unversioned markers will be used.
##
## @param FILENAME
##     The file to retrieve license contents from.
##
## @param VERSION
##     The version of the component that the license applies to.
##
get_license() {

    local FILENAME="$1"
    local VERSION="$2"
    local VERSION_MATCH='[^]]*'
    
    if [ -n "$VERSION" ]; then
        VERSION_MATCH="$(echo "$VERSION" | sed 's:[/$.*\[\\^]:\\&:g')"
    fi

    sed -n \
        -e '1,/^# --- BEGIN LICENSE FILE \(\['"$VERSION_MATCH"'\] \)\?---$/d' \
        -e '/^# --- END LICENSE FILE \(\['"$VERSION_MATCH"'\] \)\?---$/q' \
        -e 'p' \
        "$FILENAME"

}

##
## Downloads and appends the given version of the license file stored within
## the source tree at the given location. The source URL is determined by the
## value of any "Source" headers in the file. If multiple "Source" headers are
## present, each are tried in order until the download succeeds. If all
## download attempts fail, a license error is flagged.
##
## @param FILENAME
##     The file within the source tree to update. This file must contain at
##     least one "Source" header.
##
## @param VERSION
##     The version of the file to download.
##
append_license() {

    local FILENAME="$1"
    local VERSION="$2"

    get_header_value "$FILENAME" Source \
        | subst_version "$VERSION" \
        | while read -r LICENSE_URL; do

        local LICENSE="`curl -SsfL "$LICENSE_URL" | sed 's:\r$::g'`"
        if [ -n "$LICENSE" ]; then
            info "Successfully downloaded from URL \"$LICENSE_URL\"."
            printf '# --- BEGIN LICENSE FILE [%s] ---\n' "$VERSION"
            printf '%s\n' "$LICENSE"
            printf '# --- END LICENSE FILE [%s] ---\n' "$VERSION"
            exit 1
        fi

        info "Failed to download from URL \"$LICENSE_URL\"."

    done >> "$FILENAME" \
        && error "All download attempts failed for `basename "$FILENAME"` ($VERSION)."

}

# Verify that an output directory was provided
if [ -z "$DEPENDENCY_LIST" -o -z "$OUTPUT_DIR" ]; then
    error "USAGE: $0 DEPENDENCY_LIST OUTPUT_DIRECTORY"
    exit 1
fi

# Verify input file actually exists
if [ ! -r "$DEPENDENCY_LIST" ]; then
    error "$DEPENDENCY_LIST cannot be read."
    exit 1
fi

#
# Autogenerate base LICENSE and NOTICE
#

info "Processing runtime dependencies to produce LICENSE and NOTICE. Output will be within \"$OUTPUT_DIR\"."
mkdir -p "$OUTPUT_DIR"

# Copy overall license up to but not including its subcomponent preamble (the
# section that starts after a line made up entirely of "=" characters)
awk 'NR==1 {flag=1}; /^[=]{10,}$/ {flag=0}; flag;' "$BASEDIR/LICENSE" > "$OUTPUT_DIR/LICENSE"

# Copy base NOTICE verbatim
cp "$BASEDIR/NOTICE" "$OUTPUT_DIR/"

#
# Autogenerate LICENSE and NOTICE information from dependencies
#

PREAMBLE_ADDED=0
find "$DEPENDENCY_LIST" -type f -exec cat '{}' + | \
    list_dependency_license_info | sort_dependency_license_info | \
    while read LICENSE_INFO_DIR VERSION; do

    # Add subcomponent license preamble if not already added
    if [ "$PREAMBLE_ADDED" = 0 ]; then
        cat >> "$OUTPUT_DIR/LICENSE" <<EOF
==============================================================================

APACHE GUACAMOLE SUBCOMPONENTS

Apache Guacamole includes a number of subcomponents with separate copyright
notices and license terms. Your use of these subcomponents is subject to the
terms and conditions of the following licenses.

EOF
        PREAMBLE_ADDED=1
    fi

    # Refresh any outstanding licenses from defined download link
    find "$LICENSE_INFO_DIR" -type f | xargs grep -l '^#[[:space:]]*Source:' \
        | while read FILENAME; do
        if [ -z "`get_license "$FILENAME" "$VERSION"`" ]; then
            info "Downloading updated `basename "$FILENAME"` for `basename "$LICENSE_INFO_DIR"` ($VERSION)..."
            append_license "$FILENAME" "$VERSION"
        fi
    done

    # Locate LICENSE and NOTICE files
    LICENSE_FILE="`license_file "$LICENSE_INFO_DIR"`"
    NOTICE_FILE="`notice_file "$LICENSE_INFO_DIR"`"
    PRIMARY_FILE="`primary_file "$LICENSE_INFO_DIR"`"

    # Skip if primary file missing (error will be logged by primary_file
    # function call above)
    [ -n "$PRIMARY_FILE" ] || continue

    BUNDLED_LICENSE_FILE="bundled/`basename "$LICENSE_INFO_DIR"`/`basename "$LICENSE_FILE"`"
    BUNDLED_NOTICE_FILE="bundled/`basename "$LICENSE_INFO_DIR"`/`basename "$NOTICE_FILE"`"

    # Include verbatim copy of license information
    mkdir -p "$OUTPUT_DIR/bundled/"
    cp -Lr "$LICENSE_INFO_DIR" "$OUTPUT_DIR/bundled/"

    TITLE="`get_header_value "$PRIMARY_FILE" Name`"
    TITLE="$TITLE (`get_header_value "$PRIMARY_FILE" URL`)"

    # Get VERSION from header field if not otherwise available (for
    # dependencies not pulled in by Maven or NPM, like fonts)
    if [ -z "$VERSION" ]; then
        VERSION="`get_header_value "$PRIMARY_FILE" Version`"
        if [ -z "$VERSION" ]; then
            VERSION="N/A"
        fi
    fi

    (

        # Add license information to LICENSE
        printf '\n'

        cat <<EOF
$TITLE
`printf '%s' "$TITLE" | sed 's/./-/g'`

    Version: $VERSION
    From:`get_header_value "$PRIMARY_FILE" From | format_multiline`
    License(s):
EOF

        # Copy LICENSE, if provided (we omit the local copy of the LICENSE if the
        # software is Apache-licensed, as we already bundle a copy ourselves)
        printf '        '
        if [ -n "$LICENSE_FILE" ] && get_license "$LICENSE_FILE" "$VERSION" | grep -q .; then

            printf '%s (%s)\n\n' \
                "`get_header_value "$PRIMARY_FILE" License`" \
                "$BUNDLED_LICENSE_FILE"

            get_license "$LICENSE_FILE" "$VERSION" > "$OUTPUT_DIR/$BUNDLED_LICENSE_FILE"
            printf '\n' >> "$OUTPUT_DIR/$BUNDLED_LICENSE_FILE"
            get_header_value "$PRIMARY_FILE" "+" >> "$OUTPUT_DIR/$BUNDLED_LICENSE_FILE"

            # Append verbatim copy of license if small enough
            LICENSE_LINE_COUNT="`get_license "$LICENSE_FILE" "$VERSION" | wc -l`"
            if [ "$LICENSE_LINE_COUNT" -gt 0 -a "$LICENSE_LINE_COUNT" -le "$LICENSE_COPY_LIMIT" ]; then
                printf '>\n'
                get_license "$LICENSE_FILE" "$VERSION" | sed 's/^/> /' | sed 's/[[:space:]]\+$//'
                printf '>\n\n'
                get_header_value "$PRIMARY_FILE" "+" && printf '\n'
            fi

        else
            printf '%s\n\n' "`get_header_value "$PRIMARY_FILE" License`"
        fi

    ) | subst_version "$VERSION" >> "$OUTPUT_DIR/LICENSE"


    # Copy NOTICE, if provided
    if [ -n "$NOTICE_FILE" ]; then
        COMPONENT_NAME="`get_header_value "$PRIMARY_FILE" Name`"
        printf '\n======== NOTICE for "%s" ========\n\n' "$COMPONENT_NAME" >> "$OUTPUT_DIR/NOTICE"
        get_license "$NOTICE_FILE" "$VERSION" >> "$OUTPUT_DIR/NOTICE"
        get_license "$NOTICE_FILE" "$VERSION" > "$OUTPUT_DIR/$BUNDLED_NOTICE_FILE"
    fi

    # Add README describing nature of the "bundled/" directory
    cat > "$OUTPUT_DIR/bundled/README" <<EOF
Apache Guacamole includes a number of subcomponents with separate copyright
notices and license terms. Your use of these subcomponents is subject to the
terms and conditions of their respective licenses, included within this
directory for reference.
EOF

done

# Do not include internal dep-coordinates.txt mappings
for EXCLUDED_FILE in dep-coordinates.txt; do
    rm -f "$OUTPUT_DIR/bundled"/*/"$EXCLUDED_FILE"
done

# Fail if any errors occured unless explicitly configured to ignore errors with
# the IGNORE_LICENSE_ERRORS environment variable
if [ "`cat "$HAS_ERRORS"`" = "1" ]; then
    [ "$IGNORE_LICENSE_ERRORS" = "true" ] || exit 1
    info "Ignoring above license errors (IGNORE_LICENSE_ERRORS was set to \"$IGNORE_LICENSE_ERRORS\")."
else
    info "Dependency licenses processed successfully."
fi

