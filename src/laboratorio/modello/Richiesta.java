package laboratorio.modello;

/**
 * Rappresenta una richiesta di prestito effettuata da un utente per un determinato tipo di strumento.
 * 
 * Ogni richiesta contiene l'utente che l'ha effettuata, il tipo di strumento richiesto, il suo stato corrente ed eventualmente
 * il codice dello strumento assegnato all'utente.
 * 
 * Le modifiche allo stato della richiesta vengono gestite dal monitor del laboratorio per garantire la corretta
 * sincronizzazione tra i thread.
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
     * Crea una nuova richiesta di prestito.
     * 
     * La richiesta viene inizialmente creata nello stato {@link StatoRichiesta#IN_ATTESA} e non ha ancora 
     * uno strumento associato.
     * 
     * @param utente l'utente che effettua la richiesta
     * @param tipo il tipo di strumento richiesto.
     */
    public Richiesta(String utente, TipoStrumento tipo) {
        this.utente = utente;
        this.tipo = tipo;
        this.stato = StatoRichiesta.IN_ATTESA;
        this.codiceStrumento = null;
    }

    /**
     * Restituisce l'utente che ha effettuato la richiesta.
     * 
     * @return l'utente associato alla richiesta
     */
    public String getUtente() {
        return utente;
    }

    /**
     * Restituisce il tipo di strumento richiesto.
     * 
     * @return il tipo di strumento richiesto
     */
    public TipoStrumento getTipo() {
        return tipo;
    }

    /**
     * Restituisce lo stato attuale in cui si trova la richiesta.
     * 
     * @return lo stato corrente della richiesta (es. IN_ATTESA, IN_PRESTITO, ecc.)
     */
    public StatoRichiesta getStato() {
        return stato;
    }

    /**
     * Restituisce il codice identificativo dello strumento associato alla richiesta.
     * 
     * @return Il codice dello strumento, oppure {@code null} se non è ancora stato assegnato
     */
    public String getCodiceStrumento() {
        return codiceStrumento;
    }

    /**
     * Assegna uno strumento alla richiesta e aggiorna il suo stato a IN_PRESTITO.
     * @param codice il codice dello strumento assegnato
     */
    public void assegna(String codice) {
        stato = StatoRichiesta.IN_PRESTITO;
        codiceStrumento = codice;
    }

    /**
     * Aggiorna lo stato della richiesta indicando che lo strumento è stato restituito.
     */
    public void restituita() {
        stato = StatoRichiesta.RESTITUITA;
    }

    /**
     * Annulla la richiesta. 
     * Questo metodo viene usato quando la richiesta non può più essere completata, ad esempio in caso di interruzione
     * del thread in attesa.
     */
    public void annulla() {
        stato = StatoRichiesta.ANNULLATA;
    }

    /**
     * Crea una rappresentazione della richiesta da usare nell'interfaccia grafica.
     * 
     * @return una nuova istanza di {@link RichiestaView} con i dati correnti della richiesta
     */
    public RichiestaView toView() {
        return new RichiestaView(utente, tipo, stato, codiceStrumento);
    }
}