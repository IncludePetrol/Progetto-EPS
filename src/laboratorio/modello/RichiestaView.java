package laboratorio.modello;

// copia immutabile di una Richiesta
public record RichiestaView(String utente, TipoStrumento tipo, StatoRichiesta stato, String codiceStrumento) {
}
