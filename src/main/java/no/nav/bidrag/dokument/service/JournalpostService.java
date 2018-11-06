package no.nav.bidrag.dokument.service;

import no.nav.bidrag.dokument.BidragDokument;
import no.nav.bidrag.dokument.DigitUtil;
import no.nav.bidrag.dokument.consumer.BidragJournalpostConsumer;
import no.nav.bidrag.dokument.consumer.JournalforingConsumer;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.bisys.BidragJournalpostDto;
import no.nav.bidrag.dokument.dto.joark.JournalforingDto;
import no.nav.bidrag.dokument.exception.KildesystemException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Component
public class JournalpostService {

    private final BidragJournalpostConsumer bidragJournalpostConsumer;
    private final JournalforingConsumer journalforingConsumer;
    private final JournalpostMapper journalpostMapper;

    public JournalpostService(BidragJournalpostConsumer bidragJournalpostConsumer, JournalforingConsumer journalforingConsumer, JournalpostMapper journalpostMapper) {
        this.bidragJournalpostConsumer = bidragJournalpostConsumer;
        this.journalforingConsumer = journalforingConsumer;
        this.journalpostMapper = journalpostMapper;
    }

    public Optional<JournalpostDto> hentJournalpost(String journalpostId) throws KildesystemException {
        try {
            if (startsWith(BidragDokument.JOURNALPOST_ID_BIDRAG_REQUEST, journalpostId)) {
                return hentJournalpostFraBidrag(DigitUtil.tryExtraction(journalpostId));
            } else if (startsWith(BidragDokument.JOURNALPOST_ID_JOARK_REQUEST, journalpostId)) {
                return hentJournalpostFraJoark(DigitUtil.tryExtraction(journalpostId));
            }
        } catch (NumberFormatException nfe) {
            throw new KildesystemException("Kan ikke prosesseres som et tall: " + journalpostId);
        }

        throw new KildesystemException("Kunne ikke identifisere kildesystem for id: " + journalpostId);
    }

    private boolean startsWith(String prefix, String string) {
        return string != null && string.trim().toUpperCase().startsWith(prefix);
    }

    private Optional<JournalpostDto> hentJournalpostFraBidrag(Integer id) {
        Optional<BidragJournalpostDto> muligJournalpost = bidragJournalpostConsumer.hentJournalpost(id);
        return muligJournalpost.map(journalpostMapper::fraBidragJournalpost);
    }

    private Optional<JournalpostDto> hentJournalpostFraJoark(Integer id) {
        Optional<JournalforingDto> muligJournalforing = journalforingConsumer.hentJournalforing(id);
        return muligJournalforing.map(journalpostMapper::fraJournalforing);
    }

    public List<JournalpostDto> finnJournalposter(String saksnummer) {
        List<BidragJournalpostDto> bidragJournalpostDtoRequest = bidragJournalpostConsumer.finnJournalposter(saksnummer);

        return bidragJournalpostDtoRequest.stream()
                .map(journalpostMapper::fraBidragJournalpost)
                .collect(toList());
    }

    public Optional<JournalpostDto> save(JournalpostDto journalpostDto) {
        BidragJournalpostDto bidragJournalpost = journalpostMapper.tilBidragJournalpost(journalpostDto);
        return bidragJournalpostConsumer.save(bidragJournalpost)
                .map(journalpostMapper::fraBidragJournalpost);
    }
}
