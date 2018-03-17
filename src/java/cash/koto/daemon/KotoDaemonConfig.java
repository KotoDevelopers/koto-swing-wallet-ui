package cash.koto.daemon;

import com.vaklinov.zcashui.OSUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 2018-01-31
 *
 * @author KostaPC
 * c0f3.net
 */
public final class KotoDaemonConfig {

    private static final String CONFIG_PROPERTIES = "daemon.properties";

    public static final String sprout_dir = "sprout-dir";
    public static final String sprout_proving = "sprout-proving";
    public static final String sprout_verifying = "sprout-verifying";

    public static final String sprout_proving_url = "sprout-proving_url";
    public static final String sprout_verifying_url = "sprout-verifying_url";
    public static final String sprout_proving_hash = "sprout-proving-hash";
    public static final String sprout_verifying_hash = "sprout-verifying-hash";

    public static final String koto_conf_file = "koto.conf.file";


    private enum instance {
        instance(new KotoDaemonConfig());

        private KotoDaemonConfig config;

        instance(KotoDaemonConfig kotoDaemonConfig) {
            this.config = kotoDaemonConfig;
        }
    }

    public static KotoDaemonConfig config() {
        return instance.instance.config;
    }

    private Properties properties;

    private KotoDaemonConfig() {
        InputStream is = KotoDaemonConfig.class.getResourceAsStream(CONFIG_PROPERTIES);
        this.properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException e) {
            throw new StartupException(e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getSproutDir() {
        return OSUtil.getDataDirectory() + File.separator + getProperty(sprout_dir);
    }

    public String getConfigFilePath() {
        return OSUtil.getBlockchainDirectory()+File.separator+ KotoDaemonConfig.config().getProperty(KotoDaemonConfig.koto_conf_file);
    }

    public String getSproutProvingFilePath() {
        return getSproutDir()+File.separator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving);
    }

    public String getSproutVerifyingFilePath() {
        return getSproutDir()+File.separator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying);
    }


}
