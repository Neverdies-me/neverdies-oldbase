package org.nrnr.neverdies;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.apache.commons.codec.digest.DigestUtils;
import org.nrnr.neverdies.util.world.CardinalDirection;
import org.nrnr.neverdies.util.world.con1;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.nrnr.neverdies.util.Globals.mc;

public class NeverdiesMod implements ClientModInitializer {
    public static final String MOD_NAME = "Neverdies";
    public static final String MOD_VER = "1.4.0-FREE";

    @Override
    public void onInitializeClient() {
        //Shitty loader
    }
}
