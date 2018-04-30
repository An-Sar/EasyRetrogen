package de.kitsunealex.easyretrogen.init;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.kitsunealex.easyretrogen.EasyRetrogen;
import de.kitsunealex.easyretrogen.retrogen.Marker;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.util.List;

public class ModConfig {

    private static final String CATEGORY_GENERAL = "general";
    public static int MAX_CHUNKS_PER_TICK;
    public static List<Marker> MARKERS = Lists.newArrayList();

    public static void loadConfig(File file) {
        Configuration config = new Configuration(file);
        StopWatch timer = new StopWatch();
        timer.start();
        config.load();
        addProperties(config);
        config.save();
        timer.stop();
        EasyRetrogen.LOGGER.info("Loaded config in {}ms!", timer.getTime());
    }

    private static void addProperties(Configuration config) {
        MAX_CHUNKS_PER_TICK = config.get(CATEGORY_GENERAL, "maxChunksPerTick", 100).getInt();
        String[] retros = config.get(CATEGORY_GENERAL, "worldGens", new String[0]).getStringList();
        String marker = config.get(CATEGORY_GENERAL, "marker", "CPWRGMARK").getString();
        MARKERS.add(new Marker(marker, Sets.newHashSet(retros)));
    }

}
