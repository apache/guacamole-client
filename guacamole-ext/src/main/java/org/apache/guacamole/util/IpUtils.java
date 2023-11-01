package org.apache.guacamole.util;

import javax.servlet.http.HttpServletRequest;

/**
 * @author LemonZuo
 * @create 2023-11-01 20:40
 * Utility class for IP related operations.
 */
public class IpUtils {

    /**
     * Retrieves the real IP address of the client from the request.
     * This method considers the possibility of the request being passed through proxies or load balancers.
     *
     * @param request the HttpServletRequest object.
     * @return the real IP address of the client.
     */
    public static String getRealIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header, which is set by proxies and load balancers.
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            // If not available, fall back to the remote address from the request.
            ipAddress = request.getRemoteAddr();
        } else {
            // In case multiple IP addresses are returned (in cases where it has passed through multiple proxies or load balancers),
            // take only the first IP address from the list.
            ipAddress = ipAddress.split(",")[0];
        }

        return ipAddress;
    }
}

