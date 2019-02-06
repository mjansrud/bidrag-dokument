package no.nav.bidrag.dokument.consumer;

import static no.nav.bidrag.dokument.BidragDokumentConfig.ISSUER;
import static no.nav.bidrag.dokument.consumer.ConsumerUtil.addSecurityHeader;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import no.nav.bidrag.dokument.dto.EndreJournalpostCommandDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.NyJournalpostCommandDto;
import no.nav.security.oidc.context.OIDCRequestContextHolder;

public class BidragJournalpostConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BidragJournalpostConsumer.class);

    private final String baseUrlBidragJournalpost;

    private final OIDCRequestContextHolder securityContextHolder;

    public BidragJournalpostConsumer(
            String baseUrlBidragJournalpost,
            OIDCRequestContextHolder securityContextHolder) {

        this.baseUrlBidragJournalpost = baseUrlBidragJournalpost;
        this.securityContextHolder = securityContextHolder;
    }

    public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
        RestTemplate restTemplate = RestTemplateFactory.create(baseUrlBidragJournalpost);
        String path = "/sak/" + saksnummer;

        String uri = UriComponentsBuilder.fromPath(path)
                .queryParam("fagomrade", fagomrade)
                .toUriString();

        ResponseEntity<List<JournalpostDto>> journalposterForBidragRequest = restTemplate.exchange(
                uri, HttpMethod.GET, addSecurityHeader(null, getBearerToken()), typereferansenErListeMedJournalposter());

        HttpStatus httpStatus = journalposterForBidragRequest.getStatusCode();
        LOGGER.info("Fikk http status {} fra journalposter i bidragssak med saksnummer {} på fagområde {}", httpStatus, saksnummer, fagomrade);
        List<JournalpostDto> journalposter = journalposterForBidragRequest.getBody();

        return journalposter != null ? journalposter : Collections.emptyList();
    }

    private static ParameterizedTypeReference<List<JournalpostDto>> typereferansenErListeMedJournalposter() {
        return new ParameterizedTypeReference<List<JournalpostDto>>() {
        };
    }

    public Optional<JournalpostDto> registrer(NyJournalpostCommandDto nyJournalpostCommandDto) {
        RestTemplate restTemplate = RestTemplateFactory.create(baseUrlBidragJournalpost);
        String path = "/journalpost/ny";

        ResponseEntity<JournalpostDto> registrertJournalpost = restTemplate.exchange(
                path, HttpMethod.POST, addSecurityHeader(new HttpEntity<>(nyJournalpostCommandDto), getBearerToken()),
                JournalpostDto.class);

        HttpStatus httpStatus = registrertJournalpost.getStatusCode();
        LOGGER.info("Fikk http status {} fra registrer ny journalpost: {}", httpStatus, nyJournalpostCommandDto);

        return Optional.ofNullable(registrertJournalpost.getBody());
    }

    public Optional<JournalpostDto> hentJournalpost(Integer id) {
        RestTemplate restTemplate = RestTemplateFactory.create(baseUrlBidragJournalpost);
        String path = "/journalpost/" + id;

        ResponseEntity<JournalpostDto> journalpostResponseEntity = restTemplate.exchange(
                path, HttpMethod.GET, addSecurityHeader(null, getBearerToken()), JournalpostDto.class);

        HttpStatus httpStatus = journalpostResponseEntity.getStatusCode();

        LOGGER.info("JournalpostDto med id={} har http status {}", id, httpStatus);

        return Optional.ofNullable(journalpostResponseEntity.getBody());
    }

    public Optional<JournalpostDto> endre(EndreJournalpostCommandDto endreJournalpostCommandDto) {
        RestTemplate restTemplate = RestTemplateFactory.create(baseUrlBidragJournalpost);
        String path = "/journalpost";

        ResponseEntity<JournalpostDto> endretJournalpost = restTemplate.exchange(
                path, HttpMethod.POST, addSecurityHeader(new HttpEntity<>(endreJournalpostCommandDto), getBearerToken()),
                JournalpostDto.class);

        HttpStatus httpStatus = endretJournalpost.getStatusCode();
        LOGGER.info("Fikk http status {} fra endre journalpost: {}", httpStatus, endreJournalpostCommandDto);

        return Optional.ofNullable(endretJournalpost.getBody());
    }

    private String getBearerToken() {
        return "Bearer " + securityContextHolder.getOIDCValidationContext().getToken(ISSUER).getIdToken();
    }
}
