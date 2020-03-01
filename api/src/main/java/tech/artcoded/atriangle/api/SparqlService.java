package tech.artcoded.atriangle.api;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.Lang;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface SparqlService {
  Logger LOGGER = LoggerFactory.getLogger(SparqlService.class);
  String GRAPH_SUFFIX = "/graph";

  Function<String, String> CONSTRUCT_GRAPH_URI = uri -> uri + GRAPH_SUFFIX;
  Function<String, String> CLEAR_GRAPH_QUERY = graphUri -> String.format("CLEAR GRAPH <%s>", graphUri);
  Function<String, String> ASK_GRAPH_QUERY = graphUri -> String.format("ask where {graph <%s> {?s ?p ?o }}", graphUri);

  BiFunction<String, Model, String> INSERT_GRAPH_QUERY = (graphUri, model) -> {
    var writer = new StringWriter();
    model.write(writer, Lang.TURTLE.getLabel());

    return String.format("INSERT DATA { GRAPH <%s> { %s } }", graphUri, writer.toString());
  };

  SparqlServiceParam params();

  default String constructGraphUri(String uri) {
    return CONSTRUCT_GRAPH_URI.apply(uri);
  }

  default Model construct(String query) {
    return queryExecution(query, QueryExecution::execConstruct);
  }

  @SneakyThrows
  default Model construct(InputStream query) {
    return queryExecution(IOUtils.toString(query, StandardCharsets.UTF_8), QueryExecution::execConstruct);
  }


  default boolean ask(String askQuery) {
    return queryExecution(askQuery, QueryExecution::execAsk);
  }

  default boolean askGraph(String graphUri) {
    return ask(ASK_GRAPH_QUERY.apply(graphUri));
  }

  default UpdateRequest updateRequest(String... queries) {
    var updateRequest = UpdateFactory.create();
    Arrays.stream(queries)
          .filter(String::isEmpty)
          .forEach(updateRequest::add);
    return updateRequest;
  }

  @SneakyThrows
  default <T> T queryExecution(String query, Function<QueryExecution, T> queryExecutionConsumer) {
    try (
      var queryExecution = QueryExecutionFactory.sparqlService(params().getSparqlEndpointUrl(), query, params().getHttpClient())) {
      return queryExecutionConsumer.apply(queryExecution);
    }
  }

  @SneakyThrows
  default void upload(String uri, Model model) {
    try (
      RDFConnection conn = RDFConnectionRemote.create()
                                              .destination(params().getSparqlEndpointUrl())
                                              .httpClient(params().getHttpClient())
                                              .build()) {
      String graphUri = CONSTRUCT_GRAPH_URI.apply(uri);
      conn.update(CLEAR_GRAPH_QUERY.apply(graphUri));
      loadModel(ModelConverter.modelToLang(model, Lang.TURTLE).getBytes(),graphUri);
    }
  }

  private void loadModel(byte[] data, String graphUri) throws Exception {
    Authenticator.setDefault(new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(params().getUsername(), params().getPassword().toCharArray());
      }
    });

    String sparqlUrl = params().getSparqlEndpointUrl() + "-graph-crud-auth?graph-uri=" + graphUri;
    URL url = new URL(sparqlUrl);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setInstanceFollowRedirects(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/x-turtle");
    conn.setRequestProperty("charset", "utf-8");
    conn.setRequestProperty("Content-Length", Integer.toString(data.length));
    conn.setUseCaches(false);
    conn.getOutputStream().write(data);

    if ((conn.getResponseCode() / 100) != 2) throw new RuntimeException("Not 2xx as answer: " + conn.getResponseCode() + " " + conn.getResponseMessage());
  }

  default void ping() {
    Model model = this.construct("select distinct ?Concept where {[] a ?Concept} LIMIT 1");
    LOGGER.info("Virtuoso running test result: {}", ModelConverter.modelToLang(model, Lang.JSONLD));
  }
}
