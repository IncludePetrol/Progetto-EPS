package laboratorio.modello;

// uno strumento del laboratorio
// non è thread-safe: lo modifica solo il monitor Laboratorio
public class Strumento {

    private final String codice;
    private final TipoStrumento tipo;
    private StatoStrumento stato;
    private String assegnatoA;

    public Strumento(String codice, TipoStrumento tipo) {
        this.codice = codice;
        this.tipo = tipo;
        this.stato = StatoStrumento.DISPONIBILE;
        this.assegnatoA = null;
    }

    public String getCodice() {
        return codice;
    }

    public TipoStrumento getTipo() {
        return tipo;
    }

    public StatoStrumento getStato() {
        return stato;
    }

    public String getAssegnatoA() {
        return assegnatoA;
    }

    // lo presta a un utente
    public void assegna(String utente) {
        stato = StatoStrumento.IN_PRESTITO;
        assegnatoA = utente;
    }

    // lo rimette a disposizione
    public void libera() {
        stato = StatoStrumento.DISPONIBILE;
        assegnatoA = null;
    }

    // copia immutabile da passare alla GUI o al salvataggio
    public StrumentoView toView() {
        return new StrumentoView(codice, tipo, stato, assegnatoA);
    }
}
