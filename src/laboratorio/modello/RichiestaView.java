package laboratorio.modello;

/**
 * <h1>Record RichiestaView</h1>
 * 
 * Una "fotografia" immutabile e sicura di una {@link Richiesta}. 
 * Pensa a questa classe come a una versione in sola lettura (read-only) dello stato 
 * del prestito: serve per passare i dati all'interfaccia grafica o ad altre componenti 
 * esterne senza correre il rischio che qualcuno modifichi inavvertitamente i dati originali 
 * protetti dal monitor del laboratorio.
 * 
 * <p>Essendo un {@code record} Java, nasce già con tutti i campi final, costruttore 
 * e metodi di accesso pronti all'uso, riducendo al minimo il codiceboilerplate.</p>
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public record RichiestaView(
    String utente, 
    TipoStrumento tipo, 
    StatoRichiesta stato, 
    String codiceStrumento
) {
    // Il record genera già in automatico i campi, il costruttore e i metodi getter 
    // per utente(), tipo(), stato() e codiceStrumento().
}