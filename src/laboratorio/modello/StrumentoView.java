package laboratorio.modello;

// copia immutabile di uno Strumento
public record StrumentoView(String codice, TipoStrumento tipo, StatoStrumento stato, String assegnatoA) {
}
