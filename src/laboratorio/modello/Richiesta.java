package laboratorio.modello;

/**
 * <h1>Classe Richiesta</h1>
 * 
 * Rappresenta una richiesta di prestito avanzata da un utente per una certa tipologia di attrezzatura. 
 * Pensa a questo oggetto come a un "ticket" o a un modulo che viaggia nel sistema: nasce in attesa 
 * di disponibilità, può essere evaso assegnando un esemplare specifico di strumento, oppure può 
 * concludersi con la restituzione o con un annullamento (ad esempio se il thread in attesa viene interrotto).
 * 
 * <p>Nota bene sull'architettura: per garantire la sicurezza e la sincronizzazione nella gestione 
 * concorrente, lo stato di questa richiesta viene modificato esclusivamente all'interno del monitor 
 * di laboratorio.</p>
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public class Richiesta {

    private final String utente;
    private final TipoStrumento tipo;
    private StatoRichiesta stato;
    private String codiceStrumento;

    /**
     * Crea una nuova richiesta di prestito per un determinato tipo di strumento da parte di un utente.
     * 
     * <p>All'avvio, la richiesta parte automaticamente nello stato {@link StatoRichiesta#IN_ATTESA} 
     * e non ha ancora uno strumento specifico associato (il codice è vuoto).</p>
     * 
     * @invariante La richiesta nasce sempre nello stato IN_ATTESA e con codiceStrumento a null.
     * 
     * @param utente L'identificativo o il nome dell'utente che effettua la richiesta
     * @param tipo   La tipologia di strumento richiesta (es. multimetro, oscilloscopio)
     */
    public Richiesta(String utente, TipoStrumento tipo) {
        this.utente = utente;
        this.tipo = tipo;
        this.stato = StatoRichiesta.IN_ATTESA;
        this.codiceStrumento = null;
    }

    /**
     * Restituisce l'utente che ha inoltrato la richiesta di prestito.
     * 
     * @return Il nome o identificativo dell'utente
     */
    public String getUtente() {
        return utente;
    }

    /**
     * Restituisce la tipologia di strumento desiderata dall'utente.
     * 
     * @return Il {@link TipoStrumento} associato alla richiesta
     */
    public TipoStrumento getTipo() {
        return tipo;
    }

    /**
     * Restituisce lo stato attuale in cui si trova la richiesta nel ciclo di vita del prestito.
     * 
     * @return Lo {@link StatoRichiesta} corrente (es. IN_ATTESA, IN_PRESTITO, ecc.)
     */
    public StatoRichiesta getStato() {
        return stato;
    }

    /**
     * Restituisce il codice identificativo dello strumento fisico che è stato assegnato 
     * per soddisfare questa richiesta, se il prestito è attivo.
     * 
     * @return Il codice dello strumento, oppure {@code null} se non è ancora stato assegnato
     */
    public String getCodiceStrumento() {
        return codiceStrumento;
    }

    /**
     * Segna la richiesta come soddisfatta, associandole un esemplare specifico di strumento 
     * e aggiornando lo stato in corso di prestito.
     * 
     * @param codice Il codice univoco dello strumento assegnato all'utente
     */
    public void assegna(String codice) {
        stato = StatoRichiesta.IN_PRESTITO;
        codiceStrumento = codice;
    }

    /**
     * Aggiorna lo stato della richiesta indicando che lo strumento è stato regolarmente restituito.
     */
    public void restituita() {
        stato = StatoRichiesta.RESTITUITA;
    }

    /**
     * Annulla la richiesta in corso. 
     * <p>Viene solitamente invocata quando il thread in attesa viene interrotto (InterruptedException) 
     * mentre aspettava che lo strumento si liberasse.</p>
     */
    public void annulla() {
        stato = StatoRichiesta.ANNULLATA;
    }

    /**
     * Crea e restituisce un oggetto "fotografia" (View) immutabile della richiesta, 
     * utile per passare i dati all'interfaccia grafica in modo sicuro senza esporre direttamente 
     * i metodi modificabili del modello.
     * 
     * @return Un nuovo {@link RichiestaView} contenente lo stato attuale della richiesta
     */
    public RichiestaView toView() {
        return new RichiestaView(utente, tipo, stato, codiceStrumento);
    }
}