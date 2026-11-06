package laboratorio.modello;

// una richiesta di prestito fatta da un utente per un certo tipo di strumento
// come lo Strumento, viene modificata solo dentro il monitor Laboratorio
public class Richiesta {

    private final String utente;
    private final TipoStrumento tipo;
    private StatoRichiesta stato;
    private String codiceStrumento;

    public Richiesta(String utente, TipoStrumento tipo) {
        this.utente = utente;
        this.tipo = tipo;
        this.stato = StatoRichiesta.IN_ATTESA;
        this.codiceStrumento = null;
    }

    public String getUtente() {
        return utente;
    }

    public TipoStrumento getTipo() {
        return tipo;
    }

    public StatoRichiesta getStato() {
        return stato;
    }

    public String getCodiceStrumento() {
        return codiceStrumento;
    }

    // la richiesta è stata soddisfatta con lo strumento [codice]
    public void assegna(String codice) {
        stato = StatoRichiesta.IN_PRESTITO;
        codiceStrumento = codice;
    }

    public void restituita() {
        stato = StatoRichiesta.RESTITUITA;
    }

    // usata se il thread viene interrotto mentre aspetta
    public void annulla() {
        stato = StatoRichiesta.ANNULLATA;
    }

    public RichiestaView toView() {
        return new RichiestaView(utente, tipo, stato, codiceStrumento);
    }
}
