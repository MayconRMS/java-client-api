/*
 * Copyright 2019 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.dataservices;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.SessionState;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.client.io.marker.JSONWriteHandle;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;


/**
 * Manages calls to data service endpoints.
 *
 * The CallManager class is immutable and threadsafe. A single call manager can build calls for any number of endpoints.
 */
public interface CallManager {
    /**
     * A factory that constructs a call manager for a database client. The call manager makes calls
     * to the port and as the user specified when the database client was constructed using the
     * DatabaseClientFactory.
     * @param client  the database client to use when making calls
     * @return  a manager for making calls to data service endpoints
     */
    public static CallManager on(DatabaseClient client) {
        return new CallManagerImpl(client);
    }

    /**
     * A factory that constructs a session for maintaining state across calls.
     * A session can be provided on a call only if the declaration for the
     * data service endpoint specified an optional or required session. A
     * call can never take more than one session.
     * @return  a session for state associated with calls
     */
    SessionState newSessionState();

    /**
     * Starts building a call by identifying the data service endpoint to be called.
     * The declarations are often maintained as files in a project directory.
     * @param serviceDeclaration  the service.json data structure declaring the common properties of a bundle of data services
     * @param endpointDeclaration  the *.api data structure declaring the functional signature for a data service
     * @param extension  either "sjs" or "xqy" depending on whether the endpoint is implemented in server-side JavaScript or XQuery
     * @return  an endpoint object for building calls
     */
    CallableEndpoint endpoint(JSONWriteHandle serviceDeclaration, JSONWriteHandle endpointDeclaration, String extension);

    /**
     * Specifies an endpoint that can be used for building calls as returned by CallManager.endpoint()
     * The CallableEndpoint class is immutable and threadsafe.  A single CallableEndpoint can build
     * any number of calls to the endpoint.
     *
     * The next step in building a call is to construct a caller by specifying the Java representation
     * of the value or values returned by the data service endpoint. The values of the server data type
     * must be mappable to objects of the Java class.
     */
    interface CallableEndpoint {
        /**
         * Creates a caller to an endpoint that doesn't return values.
         * @return  a caller that doesn't return values
         */
        NoneCaller returningNone();
        /**
         * Creates a caller to an endpoint that returns at most one value.
         * @param as  the class for the type of the returned value such as String.class
         * @param <R>  the type of the returned value such as String
         * @return  a caller that may return a single value
         */
        <R> OneCaller<R> returningOne(Class<R> as);
        /**
         * Creates a caller to an endpoint that returns multiple values.
         * @param as  the class for the type of the returned values such as String.class
         * @param <R>  the type of the returned value such as String
         * @return  a caller that may return more than one value
         */
        <R> ManyCaller<R> returningMany(Class<R> as);
    }

    /**
     * Provides the input parameters for a specific call to a data service.  The
     * Java representation for a parameter value must be mappable to the server
     * data type for the parameter.
     */
    interface CallBuilder {
        /**
         * Sets a node parameter of the data service endpoint by means of a handle.
         * Handles provide adapters for Java IO representations and are thus
         * appropriate only for the server data types of nodes such as jsonDocument.
         * For instance, InputStreamHandle provides an adapter for InputStream.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, AbstractWriteHandle... value);
        /**
         * Sets a decimal parameter of the data service endpoint to a Java BigDecimal value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, BigDecimal... value);
        /**
         * Sets a boolean parameter of the data service endpoint to a Java boolean value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Boolean... value);
        /**
         * Sets a node parameter of the data service endpoint to a Java byte array value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, byte[]... value);
        /**
         * Sets a dateTime parameter of the data service endpoint to a Java Date value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Date... value);
        /**
         * Sets an xmlDocument parameter of the data service endpoint to a Java Document value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Document... value);
        /**
         * Sets a double parameter of the data service endpoint to a Java double value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Double... value);
        /**
         * Sets a dayTimeDuration parameter of the data service endpoint to a Java Duration value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Duration... value);
        /**
         * Sets a node parameter of the data service endpoint to a Java File value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, File... value);
        /**
         * Sets a float parameter of the data service endpoint to a Java float value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Float... value);
        /**
         * Sets an xmlDocument parameter of the data service endpoint to a Java InputSource value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, InputSource... value);
        /**
         * Sets a node parameter of the data service endpoint to a Java InputStream value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, InputStream... value);
        /**
         * Sets an int or unsignedInt parameter of the data service endpoint to a Java int value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Integer... value);
        /**
         * Sets a JSON node parameter of the data service endpoint to a Jackson JsonNode value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, JsonNode... value);
        /**
         * Sets a JSON node parameter of the data service endpoint to a Jackson JsonParser value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, JsonParser... value);
        /**
         * Sets a date parameter of the data service endpoint to a Java LocalDate value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, LocalDate... value);
        /**
         * Sets a dateTime parameter of the data service endpoint to a Java LocalDateTime value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, LocalDateTime... value);
        /**
         * Sets a time parameter of the data service endpoint to a Java LocalTime value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, LocalTime... value);
        /**
         * Sets a long or unsignedLong parameter of the data service endpoint to a Java long value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Long... value);
        /**
         * Sets a dateTime parameter of the data service endpoint to a Java OffsetDateTime value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, OffsetDateTime... value);
        /**
         * Sets a time parameter of the data service endpoint to a Java OffsetTime value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, OffsetTime... value);
        /**
         * Sets an JSON, XML, or text node parameter of the data service endpoint to a Java Reader value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Reader... value);
        /**
         * Sets an xmlDocument parameter of the data service endpoint to a Java Source value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, Source... value);
        /**
         * Sets a parameter of the data service endpoint to a Java String value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, String... value);
        /**
         * Sets an xmlDocument parameter of the data service endpoint to a Java XMLEventReader value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, XMLEventReader... value);
        /**
         * Sets an xmlDocument parameter of the data service endpoint to a Java XMLStreamReader value.
         * @param name  the name of the parameter
         * @param value  the value to assign to the parameter
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T param(String name, XMLStreamReader... value);

        /**
         * Sets the session for the call to the data service endpoint. The declaration
         * for the data service endpoint must specify that the service takes a session.
         * Only one session can be specified per call
         * @param session  a session constructed by the CallManager.newSessionState() factory
         * @param <T>  the type of caller
         * @return  the NoneCaller, OneCaller, or ManyCaller for chained building of the call
         */
        <T extends CallBuilder> T session(SessionState session);

        /**
         * Gets the path from the URI for the request to the endpoint module.
         * @return  the path
         */
        String endpointPath();
        /**
         * Identifies whether the endpoint takes a session.
         * @return  whether a session should be provided on the call
         */
        Boolean isSessionRequired();
        /**
         * Gets the definitions of the parameters available for input to a call.
         * @return  a read-only map of parameter names and definitions
         */
        Map<String, Paramdef> getParamdefs();
        /**
         * Gets the definition of the return value provided as output from a call.
         * @return  the definition of the return value
         */
        Returndef getReturndef();
    }
    interface NoneCaller extends CallBuilder {
        @Override
        NoneCaller param(String name, AbstractWriteHandle... value);
        @Override
        NoneCaller param(String name, BigDecimal... value);
        @Override
        NoneCaller param(String name, Boolean... value);
        @Override
        NoneCaller param(String name, byte[]... value);
        @Override
        NoneCaller param(String name, Date... value);
        @Override
        NoneCaller param(String name, Document... value);
        @Override
        NoneCaller param(String name, Double... value);
        @Override
        NoneCaller param(String name, Duration... value);
        @Override
        NoneCaller param(String name, File... value);
        @Override
        NoneCaller param(String name, Float... value);
        @Override
        NoneCaller param(String name, InputSource... value);
        @Override
        NoneCaller param(String name, InputStream... value);
        @Override
        NoneCaller param(String name, Integer... value);
        @Override
        NoneCaller param(String name, JsonNode... value);
        @Override
        NoneCaller param(String name, JsonParser... value);
        @Override
        NoneCaller param(String name, LocalDate... value);
        @Override
        NoneCaller param(String name, LocalDateTime... value);
        @Override
        NoneCaller param(String name, LocalTime... value);
        @Override
        NoneCaller param(String name, Long... value);
        @Override
        NoneCaller param(String name, OffsetDateTime... value);
        @Override
        NoneCaller param(String name, OffsetTime... value);
        @Override
        NoneCaller param(String name, Reader... value);
        @Override
        NoneCaller param(String name, Source... value);
        @Override
        NoneCaller param(String name, String... value);
        @Override
        NoneCaller param(String name, XMLEventReader... value);
        @Override
        NoneCaller param(String name, XMLStreamReader... value);

        @Override
        NoneCaller session(SessionState session);

        /**
         * Sends the parameter input to an endpoint module that doesn't respond with any output.
         * Can be executed any number of times to call the endpoint repeatedly with the same input.
         */
        void call();
    }
    interface OneCaller<R> extends CallBuilder {
        @Override
        OneCaller<R> param(String name, AbstractWriteHandle... value);
        @Override
        OneCaller<R> param(String name, BigDecimal... value);
        @Override
        OneCaller<R> param(String name, Boolean... value);
        @Override
        OneCaller<R> param(String name, byte[]... value);
        @Override
        OneCaller<R> param(String name, Date... value);
        @Override
        OneCaller<R> param(String name, Document... value);
        @Override
        OneCaller<R> param(String name, Double... value);
        @Override
        OneCaller<R> param(String name, Duration... value);
        @Override
        OneCaller<R> param(String name, File... value);
        @Override
        OneCaller<R> param(String name, Float... value);
        @Override
        OneCaller<R> param(String name, InputSource... value);
        @Override
        OneCaller<R> param(String name, InputStream... value);
        @Override
        OneCaller<R> param(String name, Integer... value);
        @Override
        OneCaller<R> param(String name, JsonNode... value);
        @Override
        OneCaller<R> param(String name, JsonParser... value);
        @Override
        OneCaller<R> param(String name, LocalDate... value);
        @Override
        OneCaller<R> param(String name, LocalDateTime... value);
        @Override
        OneCaller<R> param(String name, LocalTime... value);
        @Override
        OneCaller<R> param(String name, Long... value);
        @Override
        OneCaller<R> param(String name, OffsetDateTime... value);
        @Override
        OneCaller<R> param(String name, OffsetTime... value);
        @Override
        OneCaller<R> param(String name, Reader... value);
        @Override
        OneCaller<R> param(String name, Source... value);
        @Override
        OneCaller<R> param(String name, String... value);
        @Override
        OneCaller<R> param(String name, XMLEventReader... value);
        @Override
        OneCaller<R> param(String name, XMLStreamReader... value);

        @Override
        OneCaller<R> session(SessionState session);

        /**
         * Sends the parameter input to an endpoint module that responds with at most one value.
         * Can be executed any number of times to call the endpoint repeatedly with the same input.
         * @return  a Java value of the specified type as returned by the endpoint
         */
        R call();
    }
    interface ManyCaller<R> extends CallBuilder {
        @Override
        ManyCaller<R> param(String name, AbstractWriteHandle... value);
        @Override
        ManyCaller<R> param(String name, BigDecimal... value);
        @Override
        ManyCaller<R> param(String name, Boolean... value);
        @Override
        ManyCaller<R> param(String name, byte[]... value);
        @Override
        ManyCaller<R> param(String name, Date... value);
        @Override
        ManyCaller<R> param(String name, Document... value);
        @Override
        ManyCaller<R> param(String name, Double... value);
        @Override
        ManyCaller<R> param(String name, Duration... value);
        @Override
        ManyCaller<R> param(String name, File... value);
        @Override
        ManyCaller<R> param(String name, Float... value);
        @Override
        ManyCaller<R> param(String name, InputSource... value);
        @Override
        ManyCaller<R> param(String name, InputStream... value);
        @Override
        ManyCaller<R> param(String name, Integer... value);
        @Override
        ManyCaller<R> param(String name, JsonNode... value);
        @Override
        ManyCaller<R> param(String name, JsonParser... value);
        @Override
        ManyCaller<R> param(String name, LocalDate... value);
        @Override
        ManyCaller<R> param(String name, LocalDateTime... value);
        @Override
        ManyCaller<R> param(String name, LocalTime... value);
        @Override
        ManyCaller<R> param(String name, Long... value);
        @Override
        ManyCaller<R> param(String name, OffsetDateTime... value);
        @Override
        ManyCaller<R> param(String name, OffsetTime... value);
        @Override
        ManyCaller<R> param(String name, Reader... value);
        @Override
        ManyCaller<R> param(String name, Source... value);
        @Override
        ManyCaller<R> param(String name, String... value);
        @Override
        ManyCaller<R> param(String name, XMLEventReader... value);
        @Override
        ManyCaller<R> param(String name, XMLStreamReader... value);

        @Override
        ManyCaller<R> session(SessionState session);

        /**
         * Sends the parameter input to an endpoint module that can respond with multiple values.
         * Can be executed any number of times to call the endpoint repeatedly with the same input.
         * @return  a stream of Java values of the specified type as returned by the endpoint
         */
        Stream<R> call();
    }

    /**
     * Provides a read-only declaration of a parameter for the endpoint.
     */
    interface Paramdef {
        /**
         * Gets the name of the endpoint parameter.
         * @return  the parameter name
         */
        String  getParamName();
        /**
         * Gets the server data type of the endpoint parameter.
         * @return  the server data type
         */
        String  getDataType();
        /**
         * Identifies whether the endpoint parameter can take multiple values or at most one value.
         * @return  whether the parameter accepts multiple values
         */
        boolean isNullable();
        /**
         * Identifies whether a value must be provided for the endpoint parameter.
         * @return  whether the parameter is required
         */
        boolean isMultiple();
    }

    /**
     * Provides a read-only declaration of the return value for an endpoint that can return a value.
     */
    interface Returndef {
        /**
         * Gets the server data type of the output from the endpoint.
         * @return  the server data type
         */
        String  getDataType();
        /**
         * Identifies whether the endpoint can return multiple values or at most one value.
         * @return  whether the endpoint returns a stream of values
         */
        boolean isNullable();
        /**
         * Identifies whether endpoint might not produce output.
         * @return  whether the endpoint can return null
         */
        boolean isMultiple();
    }
}