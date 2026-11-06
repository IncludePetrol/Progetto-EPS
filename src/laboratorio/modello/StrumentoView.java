package laboratorio.modello;

/**
 * Copia immutabile di uno strumento, usata per passare i dati in sicurezza 
 * all'interfaccia grafica o salvarli su file senza rischiare di modificarli.
 *
 * @param codice il codice dello strumento
 * @param tipo il tipo di strumento
 * @param stato lo stato attuale (disponibile o in prestito)
 * @param assegnatoA l'utente che lo sta usando (null se è libero)
 */
public record StrumentoView(String codice, TipoStrumento tipo, StatoStrumento stato, String assegnatoA) {
}
