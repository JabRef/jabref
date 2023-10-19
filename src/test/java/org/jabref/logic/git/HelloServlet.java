package org.jabref.logic.git;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet
{
    final String greeting;

    public HelloServlet()
    {
        this("Hello");
    }

    public HelloServlet( String greeting )
    {
        this.greeting = greeting;
    }

    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response ) throws ServletException,
            IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(
                "<h1>" + greeting + " from HelloServlet</h1>");
    }
}
