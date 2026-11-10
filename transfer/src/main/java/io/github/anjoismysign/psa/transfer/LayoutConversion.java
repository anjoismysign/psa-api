package io.github.anjoismysign.psa.transfer;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Rewrites records of one table saved in an older layout into the current one,
 * so Crudable classes only need to read the current layout.
 * Implementations are found on the classpath through {@link ServiceLoader},
 * listed in 'META-INF/services/io.github.anjoismysign.psa.transfer.LayoutConversion'.
 */
public interface LayoutConversion {

    /**
     * @return the table it converts, without SQL's 'tbl' prefix, e.g. 'SerialPlayers'
     */
    @NotNull
    String table();

    /**
     * Changes the record in place.
     *
     * @return whether the record was changed
     */
    boolean convert(@NotNull JsonObject record);

    /**
     * @return table to the conversion found on the classpath
     */
    static Map<String, LayoutConversion> load() {
        Map<String, LayoutConversion> conversions = new HashMap<>();
        for (LayoutConversion conversion : ServiceLoader.load(LayoutConversion.class)) {
            LayoutConversion previous = conversions.put(conversion.table(), conversion);
            if (previous != null) {
                throw new IllegalStateException("Both " + previous.getClass().getName() + " and " + conversion.getClass().getName() + " convert '" + conversion.table() + "'");
            }
        }
        return conversions;
    }
}
