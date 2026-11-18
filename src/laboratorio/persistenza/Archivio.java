package laboratorio.persistenza;

import laboratorio.core.Laboratorio;
import laboratorio.modello.Evento;
import laboratorio.modello.Strumento;
import laboratorio.modello.StrumentoView;
import laboratorio.modello.TipoEvento;
import laboratorio.modello.TipoStrumento;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Salva e carica su file il catalogo degli strumenti e lo storico degli eventi.
 *
 * I dati sono scritti in due file di testo (UTF-8) dentro una cartella:
 * <ul>
 *   <li>{@code catalogo.csv}: una riga per strumento, nel formato {@code codice;tipo}</li>
 *   <li>{@code storico.csv}: una riga per evento, nel formato {@code orario;tipo;utente;strumento}</li>
 * </ul>
 * Le righe vuote e quelle che iniziano con {@code #} vengono ignorate in lettura.
 *
 * Scelte di progetto:
 * <ul>
 *   <li>la classe non tocca mai il monitor mentre fa I/O: {@link #salva(Laboratorio)} prende prima
 *       una copia immutabile ({@code snapshot()}, {@code getStorico()}) e scrive solo dopo;
 *       {@link #carica(Laboratorio)} legge e valida tutto prima di chiamare {@code ripristina()};</li>
 *   <li>nel catalogo si salvano solo codice e tipo: lo stato di prestito non è persistente, perché
 *       al riavvio non ci sono thread utente e quindi nessuno strumento può essere in prestito;</li>
 *   <li>la scrittura avviene su un file temporaneo poi rinominato, così un errore a metà
 *       non lascia un file di salvataggio troncato;</li>
 *   <li>un file malformato non viene mai caricato a metà: si lancia {@link IOException}
 *       con il numero di riga e il laboratorio resta invariato.</li>
 * </ul>
 */
public class Archivio {

    /** Nome del file che contiene il catalogo. */
    public static final String FILE_CATALOGO = "catalogo.csv";
    /** Nome del file che contiene lo storico. */
    public static final String FILE_STORICO = "storico.csv";

    private static final String SEP = ";";

    private final Path catalogo;
    private final Path storico;

    /**
     * Crea un archivio che usa i file standard dentro la cartella indicata.
     * La cartella viene creata al primo salvataggio se non esiste.
     *
     * @param cartella la cartella in cui si trovano (o verranno creati) i file
     */
    public Archivio(Path cartella) {
        this.catalogo = cartella.resolve(FILE_CATALOGO);
        this.storico = cartella.resolve(FILE_STORICO);
    }

    // ---------- operazioni sul laboratorio ----------

    /**
     * Salva catalogo e storico del laboratorio. Il monitor viene occupato solo il tempo
     * di copiare lo stato; la scrittura su disco avviene dopo averlo rilasciato.
     *
     * @param lab il laboratorio da salvare
     * @throws IOException se la scrittura fallisce
     */
    public void salva(Laboratorio lab) throws IOException {
        List<StrumentoView> strumenti = lab.snapshot().strumenti();
        List<Evento> eventi = lab.getRegistro().getStorico();
        salvaCatalogo(strumenti);
        salvaStorico(eventi);
    }

    /**
     * Carica catalogo e storico dai file e li installa nel laboratorio. I file che non
     * esistono vengono saltati (primo avvio). Tutta la lettura e la validazione avvengono
     * prima di toccare il laboratorio: se qualcosa è malformato non cambia nulla.
     *
     * @param lab il laboratorio in cui caricare i dati
     * @throws IOException se un file è illeggibile o malformato
     */
    public void carica(Laboratorio lab) throws IOException {
        List<Strumento> strumenti = Files.exists(catalogo) ? caricaCatalogo() : null;
        List<Evento> eventi = Files.exists(storico) ? caricaStorico() : null;
        if (strumenti != null) {
            lab.ripristina(strumenti); // valida capienza e codici duplicati
        }
        if (eventi != null) {
            lab.getRegistro().ripristina(eventi);
        }
    }

    // ---------- catalogo ----------

    /**
     * Scrive il catalogo su file (solo codice e tipo di ogni strumento).
     *
     * @param strumenti gli strumenti da salvare
     * @throws IOException se la scrittura fallisce
     */
    public void salvaCatalogo(List<StrumentoView> strumenti) throws IOException {
        List<String> righe = new ArrayList<>();
        righe.add("# codice;tipo");
        for (StrumentoView s : strumenti) {
            righe.add(campo(s.codice()) + SEP + s.tipo().name());
        }
        scrivi(catalogo, righe);
    }

    /**
     * Legge il catalogo dal file. Gli strumenti restituiti sono tutti disponibili.
     *
     * @return la lista degli strumenti letti
     * @throws IOException se il file non si legge o contiene righe non valide
     */
    public List<Strumento> caricaCatalogo() throws IOException {
        List<Strumento> out = new ArrayList<>();
        int n = 0;
        for (String riga : Files.readAllLines(catalogo, StandardCharsets.UTF_8)) {
            n++;
            if (saltare(riga)) {
                continue;
            }
            String[] p = riga.split(SEP, -1);
            if (p.length != 2 || p[0].isBlank()) {
                throw errore(catalogo, n, "attesi due campi (codice;tipo)");
            }
            try {
                out.add(new Strumento(p[0].trim(), TipoStrumento.valueOf(p[1].trim())));
            } catch (IllegalArgumentException e) {
                throw errore(catalogo, n, "tipo sconosciuto: " + p[1]);
            }
        }
        return out;
    }

    // ---------- storico ----------

    /**
     * Scrive lo storico su file.
     *
     * @param eventi gli eventi da salvare
     * @throws IOException se la scrittura fallisce
     */
    public void salvaStorico(List<Evento> eventi) throws IOException {
        List<String> righe = new ArrayList<>();
        righe.add("# orario;tipo;utente;strumento");
        for (Evento e : eventi) {
            righe.add(e.orario() + SEP + e.tipo().name() + SEP + campo(e.utente()) + SEP + campo(e.strumento()));
        }
        scrivi(storico, righe);
    }

    /**
     * Legge lo storico dal file.
     *
     * @return la lista degli eventi letti, nell'ordine del file
     * @throws IOException se il file non si legge o contiene righe non valide
     */
    public List<Evento> caricaStorico() throws IOException {
        List<Evento> out = new ArrayList<>();
        int n = 0;
        for (String riga : Files.readAllLines(storico, StandardCharsets.UTF_8)) {
            n++;
            if (saltare(riga)) {
                continue;
            }
            String[] p = riga.split(SEP, -1);
            if (p.length != 4) {
                throw errore(storico, n, "attesi quattro campi (orario;tipo;utente;strumento)");
            }
            try {
                out.add(new Evento(LocalTime.parse(p[0].trim()), TipoEvento.valueOf(p[1].trim()), p[2], p[3]));
            } catch (DateTimeParseException e) {
                throw errore(storico, n, "orario non valido: " + p[0]);
            } catch (IllegalArgumentException e) {
                throw errore(storico, n, "tipo di evento sconosciuto: " + p[1]);
            }
        }
        return out;
    }

    // ---------- supporto ----------

    private static boolean saltare(String riga) {
        return riga.isBlank() || riga.startsWith("#");
    }

    // i campi di testo non possono contenere il separatore o ritorni a capo, altrimenti il file diventa ambiguo
    private static String campo(String valore) throws IOException {
        if (valore.contains(SEP) || valore.contains("\n") || valore.contains("\r")) {
            throw new IOException("valore non salvabile (contiene ';' o a capo): " + valore);
        }
        return valore;
    }

    private static IOException errore(Path file, int riga, String motivo) {
        return new IOException(file.getFileName() + ", riga " + riga + ": " + motivo);
    }

    // scrive su un file temporaneo e poi lo sposta sul definitivo
    private static void scrivi(Path destinazione, List<String> righe) throws IOException {
        Path cartella = destinazione.toAbsolutePath().getParent();
        Files.createDirectories(cartella);
        Path tmp = Files.createTempFile(cartella, destinazione.getFileName().toString(), ".tmp");
        try {
            Files.write(tmp, righe, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, destinazione, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, destinazione, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
