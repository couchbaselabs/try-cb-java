package trycb.service;

public class Index {
    /**
     * Returns the index page.
     */
    public static String getInfo() {
        return "<h1> Java Travel Sample API </h1>"
                + "A sample API for getting started with Couchbase Server and the Java SDK." + "<ul>"
                + "<li> <a href = \"/apidocs\"> Learn the API with Swagger, interactively </a>"
                + "<li> <a href = \"https://github.com/couchbaselabs/try-cb-java\"> GitHub </a>" + "</ul>";
    }
}
