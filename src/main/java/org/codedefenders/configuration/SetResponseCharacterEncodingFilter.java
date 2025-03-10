package org.codedefenders.configuration;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

/**
 * Set response encoding to UTF-8.
 */
@WebFilter(filterName = "ResponseCharacterEncoding")
public class SetResponseCharacterEncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        filterChain.doFilter(request, response);
    }
}
