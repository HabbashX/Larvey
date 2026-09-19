package com.habbashx.larvey.example;

import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.mapper.LarveyMapper;
import com.habbashx.larvey.mapper.MappingStrategy;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Main {
    public static void main(String[] args) throws Exception {
        String source;
        try (InputStream input = Main.class.getResourceAsStream("/application.larvey")) {
            if (input == null) {
                throw new IllegalStateException("application.larvey not found");
            }
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        ApplicationConfig reflection = Larvey.parse(source).map(ApplicationConfig.class);
        print("reflection", reflection);
        LarveyMapper bytecode = Larvey.builder().strategy(MappingStrategy.BYTECODE).build();
        ApplicationConfig generated = bytecode.map(Larvey.parseAst(source), ApplicationConfig.class);
        print("bytecode", generated);
        System.out.println(Larvey.write(reflection));
    }

    private static void print(String mode, ApplicationConfig config) {
        System.out.println("[" + mode + "] " + config.getName() + " " + config.getVersion() + " debug=" + config.isDebug());
        System.out.println("[" + mode + "] server=" + config.getServer().getHost() + ":" + config.getServer().getPort());
        System.out.println("[" + mode + "] database=" + config.getDatabase().getDriver() + "://" + config.getDatabase().getUsername() + "@" + config.getDatabase().getHost() + ":" + config.getDatabase().getPort() + "/" + config.getDatabase().getDatabase());
    }
}
