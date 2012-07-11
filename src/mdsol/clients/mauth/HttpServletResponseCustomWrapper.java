package mdsol.clients.mauth;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HttpServletResponseCustomWrapper extends HttpServletResponseWrapper
{
	private int status;

	public HttpServletResponseCustomWrapper(HttpServletResponse response)
	{
        super(response);
    }

	private StringWriter sw = new StringWriter();
	
	public PrintWriter getWriter() throws IOException
	{
		return new PrintWriter(sw);
	}
	
	public ServletOutputStream getOutputStream() throws IOException
	{
		throw new UnsupportedOperationException();
	}
	
	public String getBody()
	{
		return sw.toString();
	}
	
	
    @Override
    public void setStatus(int sc)
    {
        super.setStatus(sc);
        status = sc;
    }

    public int getStatus()
    {
        return status;
    }    
}
