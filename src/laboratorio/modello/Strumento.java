package laboratorio.modello;

/**
 * Rappresenta un singolo strumento all'interno del laboratorio.
 * Nota di progetto: questa classe non è thread-safe per scelta progettuale; 
 * le modifiche al suo stato devono essere gestite in modo esclusivo dal monitor {@link laboratorio.core.Laboratorio}.
 */
public class Strumento {

    private final String codice;
    private final TipoStrumento tipo;
    private StatoStrumento stato;
    private String assegnatoA;

    /**
     * Crea un nuovo strumento e lo imposta di default come disponibile.
     *
     * @param codice l'identificativo univoco dello strumento
     * @param tipo la tipologia dello strumento
     */
    public Strumento(String codice, TipoStrumento tipo) {
        this.codice = codice;
        this.tipo = tipo;
        this.stato = StatoStrumento.DISPONIBILE;
        this.assegnatoA = null;
    }

    /**
     * @return il codice univoco dello strumento
     */
    public String getCodice() {
        return codice;
    }

    /**
     * @return la tipologia dello strumento
     */
    public TipoStrumento getTipo() {
        return tipo;
    }

    /**
     * @return lo stato attuale di disponibilità dello strumento
     */
    public StatoStrumento getStato() {
        return stato;
    }

    /**
     * @return il nome dell'utente che ha attualmente in prestito lo strumento, oppure null se è libero
     */
    public String getAssegnatoA() {
        return assegnatoA;
    }

    /**
     * Assegna lo strumento a un utente, segnandolo come in prestito.
     *
     * @param utente il nome della persona a cui viene prestato lo strumento
     */
    public void assegna(String utente) {
        stato = StatoStrumento.IN_PRESTITO;
        assegnatoA = utente;
    }

    /**
     * Registra la restituzione dello strumento, svincolandolo dall'utente e 
     * rendendolo nuovamente disponibile per altri prestiti.
     */
    public void libera() {
        stato = StatoStrumento.DISPONIBILE;
        assegnatoA = null;
    }

    /**
     * Genera una fotografia immutabile dello strumento.
     * È molto utile per passare i dati in sicurezza all'interfaccia grafica (GUI) 
     * o per il salvataggio, evitando modifiche accidentali da altre parti del codice.
     *
     * @return un oggetto StrumentoView con i dati attuali
     */
    public StrumentoView toView() {
        return new StrumentoView(codice, tipo, stato, assegnatoA);
    }
}
