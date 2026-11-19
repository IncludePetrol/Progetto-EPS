package laboratorio.persistenza;

import laboratorio.core.Laboratorio;
import laboratorio.modello.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link Archivio}.
 *
 * Verifica il salvataggio e il caricamento di catalogo e storico su file CSV,
 * la gestione del primo avvio (file assenti) e la robustezza contro file malformati:
 * un file non valido non deve mai lasciare il laboratorio caricato a metà.
 */
class ArchivioTest {

    private static final TipoStrumento OSC = TipoStrumento.OSCILLOSCOPIO;

    /** Cartella temporanea creata da JUnit per ogni test e cancellata alla fine. */
    @TempDir
    Path cartella;

    private Archivio archivio;
    private Laboratorio lab;

    /**
     * Prepara un archivio su una cartella vuota e un laboratorio con due strumenti.
     */
    @BeforeEach
    void setUp() {
        archivio = new Archivio(cartella);
        lab = new Laboratorio();
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.aggiungiStrumento(new Strumento("GEN-01", TipoStrumento.GENERATORE));
    }

    /**
     * Metodo di utilità per scrivere a mano il contenuto di uno dei file dell'archivio.
     *
     * @param nome il nome del file dentro la cartella
     * @param righe le righe da scrivere
     * @throws IOException se la scrittura fallisce
     */
    private void scriviFile(String nome, String... righe) throws IOException {
        Files.write(cartella.resolve(nome), List.of(righe), StandardCharsets.UTF_8);
    }

    // ---------- salvataggio e caricamento ----------

    /**
     * Salvando un laboratorio e ricaricandolo in uno nuovo si devono ritrovare
     * gli stessi strumenti e lo stesso storico.
     */
    @Test
    void salvareERicaricareRestituisceGliStessiDati() throws Exception {
        lab.richiedi("Alysa", OSC);
        lab.restituisci("Alysa", "OSC-01");
        List<Evento> storico = lab.getRegistro().getStorico();

        archivio.salva(lab);
        Laboratorio nuovo = new Laboratorio();
        archivio.carica(nuovo);

        assertEquals(lab.snapshot().strumenti(), nuovo.snapshot().strumenti());
        assertEquals(storico, nuovo.getRegistro().getStorico());
    }

    /**
     * Lo stato di prestito non è persistente: uno strumento salvato mentre è in prestito
     * deve tornare disponibile al caricamento.
     */
    @Test
    void gliStrumentiCaricatiSonoTuttiDisponibili() throws Exception {
        lab.richiedi("Alysa", OSC);

        archivio.salva(lab);
        Laboratorio nuovo = new Laboratorio();
        archivio.carica(nuovo);

        StatoLab stato = nuovo.snapshot();
        assertEquals(2, stato.disponibili());
        assertEquals(0, stato.inPrestito());
    }

    /**
     * Al primo avvio i file non esistono: il caricamento non deve dare errore
     * e il laboratorio deve restare com'era.
     */
    @Test
    void senzaFileIlLaboratorioRestaInvariato() throws Exception {
        archivio.carica(lab);

        assertEquals(2, lab.snapshot().strumenti().size());
        assertTrue(lab.getRegistro().getStorico().isEmpty());
    }

    /**
     * Il salvataggio deve creare la cartella se non esiste ancora.
     */
    @Test
    void ilSalvataggioCreaLaCartella() throws Exception {
        Path sottocartella = cartella.resolve("dati");
        new Archivio(sottocartella).salva(lab);

        assertTrue(Files.exists(sottocartella.resolve(Archivio.FILE_CATALOGO)));
        assertTrue(Files.exists(sottocartella.resolve(Archivio.FILE_STORICO)));
    }

    /**
     * Dopo il salvataggio non devono restare file temporanei nella cartella.
     */
    @Test
    void ilSalvataggioNonLasciaFileTemporanei() throws Exception {
        archivio.salva(lab);

        try (var file = Files.list(cartella)) {
            assertEquals(2, file.count());
        }
    }

    // ---------- lettura dei file ----------

    /**
     * Le righe vuote e i commenti che iniziano con # devono essere ignorati.
     */
    @Test
    void righeVuoteECommentiVengonoIgnorati() throws Exception {
        scriviFile(Archivio.FILE_CATALOGO, "# commento", "", "OSC-01;OSCILLOSCOPIO", "   ");

        List<Strumento> strumenti = archivio.caricaCatalogo();

        assertEquals(1, strumenti.size());
        assertEquals("OSC-01", strumenti.get(0).getCodice());
        assertEquals(OSC, strumenti.get(0).getTipo());
    }

    /**
     * Lo storico letto deve contenere gli eventi nell'ordine del file.
     */
    @Test
    void loStoricoVieneLettoInOrdine() throws Exception {
        scriviFile(Archivio.FILE_STORICO,
                "10:00;ASSEGNAZIONE;Alysa;OSC-01",
                "10:05;RESTITUZIONE;Alysa;OSC-01");

        assertEquals(List.of(
                new Evento(LocalTime.of(10, 0), TipoEvento.ASSEGNAZIONE, "Alysa", "OSC-01"),
                new Evento(LocalTime.of(10, 5), TipoEvento.RESTITUZIONE, "Alysa", "OSC-01")),
                archivio.caricaStorico());
    }

    // ---------- file malformati ----------

    /**
     * Una riga del catalogo con un numero di campi sbagliato deve dare errore
     * indicando il numero di riga.
     */
    @Test
    void unCatalogoConCampiMancantiDaErrore() throws Exception {
        scriviFile(Archivio.FILE_CATALOGO, "# codice;tipo", "OSC-01");

        IOException e = assertThrows(IOException.class, archivio::caricaCatalogo);
        assertTrue(e.getMessage().contains("riga 2"), e.getMessage());
    }

    /**
     * Un tipo di strumento che non esiste deve dare errore.
     */
    @Test
    void unTipoDiStrumentoSconosciutoDaErrore() throws Exception {
        scriviFile(Archivio.FILE_CATALOGO, "XYZ-01;TOSTAPANE");

        assertThrows(IOException.class, archivio::caricaCatalogo);
    }

    /**
     * Un orario non valido nello storico deve dare errore.
     */
    @Test
    void unOrarioNonValidoDaErrore() throws Exception {
        scriviFile(Archivio.FILE_STORICO, "25:99;ASSEGNAZIONE;Alysa;OSC-01");

        assertThrows(IOException.class, archivio::caricaStorico);
    }

    /**
     * Un tipo di evento che non esiste deve dare errore.
     */
    @Test
    void unTipoDiEventoSconosciutoDaErrore() throws Exception {
        scriviFile(Archivio.FILE_STORICO, "10:00;FESTA;Alysa;OSC-01");

        assertThrows(IOException.class, archivio::caricaStorico);
    }

    /**
     * Se lo storico è malformato il caricamento deve fallire senza toccare nulla,
     * nemmeno il catalogo che invece era valido.
     */
    @Test
    void unFileMalformatoNonCaricaNulla() throws Exception {
        scriviFile(Archivio.FILE_CATALOGO, "MUL-01;MULTIMETRO");
        scriviFile(Archivio.FILE_STORICO, "riga senza senso");

        assertThrows(IOException.class, () -> archivio.carica(lab));

        List<StrumentoView> strumenti = lab.snapshot().strumenti();
        assertEquals(2, strumenti.size());
        assertEquals("OSC-01", strumenti.get(0).codice());
    }

    /**
     * Un valore che contiene il separatore renderebbe il file ambiguo: non deve essere salvato.
     */
    @Test
    void unValoreConIlSeparatoreNonSiPuoSalvare() {
        List<Evento> eventi = List.of(
                new Evento(LocalTime.of(10, 0), TipoEvento.ASSEGNAZIONE, "Aly;sa", "OSC-01"));

        assertThrows(IOException.class, () -> archivio.salvaStorico(eventi));
    }
}
