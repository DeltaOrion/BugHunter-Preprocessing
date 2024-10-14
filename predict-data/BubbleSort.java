package au.com.skript.api.consent;

import au.com.skript.api.consent.entities.Organisation;
import au.com.skript.api.consent.entities.collection.CollectionCategory;
import au.com.skript.api.consent.entities.collection.CollectionStatisticsResponse;
import au.com.skript.api.consent.entities.consent.v1.ConsentApi;
import au.com.skript.api.consent.entities.consent.v1.ConsentStatus;
import au.com.skript.micronaut.util.api.ResponseStatus;
import au.com.skript.micronaut.util.api.SkriptItemResponse;
import au.com.skript.micronaut.util.api.SkriptQueryRequest;
import au.com.skript.micronaut.util.api.SkriptQueryRequestParam;
import au.com.skript.micronaut.util.http.HttpUtil;
import au.com.skript.micronaut.util.http.IfModSinceHeaderValidate;
import au.com.skript.micronaut.util.http.ResponseUtil;
import au.com.skript.micronaut.util.http.Scopes;
import au.com.skript.micronaut.util.http.SkriptHttpException;
import au.com.skript.micronaut.util.http.UuidParamValidate;
import au.com.skript.micronaut.util.http.auth.entities.ApiCredential;
import au.com.skript.micronaut.util.otel.NatsHelper;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller("/v1/managed-organisations/{orgId}/consumers/{consumerId}/consents")
public class ManagedOrgConsentsController {
    // default limit
    private static final int LIMIT = 50;
    private static final Set<ConsentStatus> COLLECTABLE_STATUSES =
            Set.of(ConsentStatus.ACTIVE, ConsentStatus.EXPIRED, ConsentStatus.REVOKED);

    @Inject private ApiProvider apiProvider;
    @Inject private SenderClient senderClient;
    @Inject private ResponseUtil responseUtil;
    @Inject private ConsentApiConverterV1 consentApiConverter;

    @Get
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<List<ConsentApi>> getConsents(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @SkriptQueryRequestParam(
                    defaultSize = LIMIT,
                    defaultProjection = {
                            "id",
                            "parentId",
                            "dataHolderId",
                            "status",
                            "consentExpiry",
                            "scopes"
                    },
                    responseType = ConsentApi.class)
            SkriptQueryRequest<UUID> request) {
        return apiProvider.getConsents(httpRequest, validateOrgId(httpRequest, orgId), request);
    }

    @Get(uri = "/{id}")
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<ConsentApi> getConsent(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @PathVariable @UuidParamValidate String id,
            @Header(HttpHeaders.IF_MODIFIED_SINCE) @IfModSinceHeaderValidate
            Optional<String> ifModifiedSince) {
        return apiProvider.getConsent(
                httpRequest, validateOrgId(httpRequest, orgId), id, ifModifiedSince);
    }

    @Get(uri = "/{id}/collections")
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<List<CollectionCategory>> getCollections(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @PathVariable @UuidParamValidate String id) {
        return apiProvider.getCollections(httpRequest, validateOrgId(httpRequest, orgId), id);
    }

    @Get(uri = "/{id}/collections/customer")
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<CollectionStatisticsResponse> getCustomerCollectionStat(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @PathVariable @UuidParamValidate String id) {
        return apiProvider.getCustomerCollectionStat(
                httpRequest, validateOrgId(httpRequest, orgId), id);
    }

    @Get(uri = "/{id}/collections/accounts")
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<CollectionStatisticsResponse> getAccountsCollectionStat(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @PathVariable @UuidParamValidate String id) {
        return apiProvider.getAccountsCollectionStat(
                httpRequest, validateOrgId(httpRequest, orgId), id);
    }

    @Get(uri = "/{id}/collections/transactions")
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<CollectionStatisticsResponse> getTransactionsCollectionStat(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @PathVariable @UuidParamValidate String id) {
        return apiProvider.getTransactionsCollectionStat(
                httpRequest, validateOrgId(httpRequest, orgId), id);
    }

    @Get(uri = "/{id}/collections/scheduled-payments")
    @Scopes({"skript/managed-organisations"})
    public HttpResponse<CollectionStatisticsResponse> getScheduledPaymentsCollectionStat(
            HttpRequest<?> httpRequest,
            @PathVariable @UuidParamValidate String orgId,
            @PathVariable @UuidParamValidate String id) {
        return apiProvider.getScheduledPaymentsCollectionStat(
                httpRequest, validateOrgId(httpRequest, orgId), id);
    }

    private String validateOrgId(HttpRequest<?> httpRequest, String orgId) {
        ApiCredential credential = HttpUtil.getApiCredential(httpRequest);
        SkriptItemResponse<Organisation> response =
                NatsHelper.natsWithClientSpan(
                        "organisations.v1.fetch",
                        () ->
                                senderClient.fetchOrganisation(
                                        new Organisation().setId(orgId), NatsHelper.createPropagationHeaders()));
        if (response.getStatus() != ResponseStatus.OK
                || !credential.getOrgId().equals(response.getData().getManagerOrganisationId())) {
            throw new SkriptHttpException(HttpStatus.UNAUTHORIZED, "ERR-000001", "Security error");
        }
        return orgId;
    }
}
