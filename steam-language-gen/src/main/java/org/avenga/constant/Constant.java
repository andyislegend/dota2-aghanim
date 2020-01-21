package org.avenga.constant;

public class Constant {

    public static final String BASE_PACKAGE_NAME = "com.avenga.steamclient";
    public static final String OUTPUT_DIR = "generated/source/steamd/main/java/" + BASE_PACKAGE_NAME.replaceAll("\\.", "/");
    public static final String INPUT_DIR = "src/main/steamd/" + BASE_PACKAGE_NAME.replaceAll("\\.", "/") + "/steammsg.steamd";

    public static final String UTILITY_CLASS_INIT_ERROR = "Instance of this class can't be initialized!";

    private Constant() {
        throw new IllegalStateException(UTILITY_CLASS_INIT_ERROR);
    }
}
