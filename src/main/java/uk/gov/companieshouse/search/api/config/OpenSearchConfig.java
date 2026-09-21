package uk.gov.companieshouse.search.api.config;

import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.search.api.exception.EndpointException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static uk.gov.companieshouse.search.api.logging.LoggingUtils.getLogger;

@Configuration
public class OpenSearchConfig {

    private final EnvironmentReader environmentReader;

    public OpenSearchConfig(EnvironmentReader environmentReader) {
        this.environmentReader = environmentReader;
    }

    private static final String ALPHABETICAL_SEARCH_URL = "ALPHABETICAL_SEARCH_URL";

    // IAM action/service name prefix used by Amazon OpenSearch Service for SigV4 signing (e.g. es:ESHttpPost)
    private static final String OPENSEARCH_SIGNING_SERVICE_NAME = "es";

    @Bean
    public OpenSearchClient alphabeticalSearchRestClient() {
        return createOpenSearchClient(ALPHABETICAL_SEARCH_URL);
    }

    public OpenSearchClient createOpenSearchClient(String url) {
        URL endpoint;

        try {
            String rawUrl = environmentReader.getMandatoryString(url);
            URI uri = new URI(rawUrl);
            endpoint = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new EndpointException(
                    url + " environment variable is malformed; expected format is <protocol>://<host>[:port]"
            );
        }

        SdkHttpClient httpClient = ApacheHttpClient.builder().build();
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();
        Region region = DefaultAwsRegionProviderChain.builder().build().getRegion();

        getLogger().info("Region is: " + region);

        OpenSearchTransport transport = new AwsSdk2Transport(
                httpClient,
                endpoint.getHost(),
                OPENSEARCH_SIGNING_SERVICE_NAME,
                region,
                AwsSdk2TransportOptions.builder()
                        .setMapper(new JacksonJsonpMapper())
                        .setCredentials(credentialsProvider)
                        .build()
        );

        return new OpenSearchClient(transport);
    }
}
