package com.metispro.example.rest.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import com.metispro.example.rest.JsonBean;

public class HelloWorldTest
{
    private static String endpointUrl;

    @BeforeClass
    public static void beforeClass()
    {
        endpointUrl = System.getProperty("service.url",
                "http://localhost:8080/metispro-registration");
    }

    @Test
    public void testInvalidURL() throws Exception
    {
        // UnknownHostException
        try
        {
            WebClient client = WebClient
                    .create("http://bogus.url.bogus/hello/echo/blah");
            Response r = client.accept("text/plain").get();
            assertTrue(false);
        } catch (ClientException e)
        {
            System.out.println(e.getMessage());
            assertTrue(true);
        }

        // HTTP 404 test
        try
        {
            WebClient client = WebClient.create("http://localhost:8080/blah");
            Response r = client.accept("text/plain").get();
            System.out.println(r.getStatus() + " "
                    + r.getStatusInfo().getReasonPhrase());
            assertTrue(r.getStatus() == 404);
        } catch (ClientException e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testPing() throws Exception
    {
        String echo = "SierraTangoNevada";
        WebClient client = WebClient
                .create(endpointUrl + "/hello/echo/" + echo);
        Response r = client.accept("text/plain").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        String value = IOUtils.toString((InputStream) r.getEntity());
        assertEquals(echo, value);
        System.out.println(value);
    }

    @Test
    public void testJsonRoundtrip() throws Exception
    {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());
        JsonBean inputBean = new JsonBean();
        inputBean.setVal1("Maple");

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = mapper.writeValueAsString(inputBean);
        System.out.println("Sending JsonBean:\n " + json);

        WebClient client = WebClient.create(endpointUrl + "/hello/jsonBean",
                providers);
        Response r = client.accept("application/json").type("application/json")
                .post(inputBean);
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

        MappingJsonFactory factory = new MappingJsonFactory();
        byte[] bytes = readStream((InputStream) r.getEntity());
        JsonParser parser = factory.createJsonParser(bytes);

        System.out.println("Response:\n " + new String(bytes));
        JsonBean output = parser.readValueAs(JsonBean.class);
        assertEquals("Maple", output.getVal2());
    }

    private static byte[] readStream(InputStream in) throws IOException
    {
        byte[] buf = new byte[1024];
        int count = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        while ((count = in.read(buf)) != -1)
            out.write(buf, 0, count);
        return out.toByteArray();
    }
}
