/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import static org.sonar.application.config.SonarQubeVersionHelper.getSonarqubeVersion;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.sonar.application.command.CommandFactory;
import org.sonar.application.command.CommandFactoryImpl;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.AppSettingsLoader;
import org.sonar.application.config.AppSettingsLoaderImpl;
import org.sonar.application.process.ProcessLauncher;
import org.sonar.application.process.ProcessLauncherImpl;
import org.sonar.application.process.StopRequestWatcher;
import org.sonar.application.process.StopRequestWatcherImpl;
import org.sonar.process.System2;
import org.sonar.process.SystemExit;

public class App {

  private final SystemExit systemExit = new SystemExit();
  private StopRequestWatcher stopRequestWatcher;

  public void start(String[] cliArguments) throws IOException {
	File out = new File("conf/sonar.properties");
	if ( !out.exists() ) {
		Properties props = new Properties();
		props.load(App.class.getResourceAsStream("/default.sonar.properties"));
		for ( Entry<String, String> e : System.getenv().entrySet() ) {
			if ( e.getKey().startsWith("prop.") ) {
				props.put(e.getKey().substring(5), e.getValue());
			}
		}
		for ( Entry<java.lang.Object,java.lang.Object> e : System.getProperties().entrySet() ) {
			if ( ((String)e.getKey()).startsWith("prop.") ) {
				props.put(((String)e.getKey()).substring(5), (String)e.getValue());
			}
		}
		if ( !out.getParentFile().exists() ) {
			out.getParentFile().mkdir();
		}
		props.store(new FileOutputStream(out), "#default and overwritten properties from environment");
	}
	
	  
	  
    AppSettingsLoader settingsLoader = new AppSettingsLoaderImpl(cliArguments);
    AppSettings settings = settingsLoader.load();
    // order is important - logging must be configured before any other components (AppFileSystem, ...)
    AppLogging logging = new AppLogging(settings);
    logging.configure();
    AppFileSystem fileSystem = new AppFileSystem(settings);

    try (AppState appState = new AppStateFactory(settings).create()) {
      appState.registerSonarQubeVersion(getSonarqubeVersion());
      appState.registerClusterName(settings.getProps().nonNullValue(CLUSTER_NAME.getKey()));
      AppReloader appReloader = new AppReloaderImpl(settingsLoader, fileSystem, appState, logging);
      fileSystem.reset();
      CommandFactory commandFactory = new CommandFactoryImpl(settings.getProps(), fileSystem.getTempDir(), System2.INSTANCE);

      try (ProcessLauncher processLauncher = new ProcessLauncherImpl(fileSystem.getTempDir())) {
        Scheduler scheduler = new CodeScanSchedulerImpl(settings, appReloader, commandFactory, processLauncher, appState);

        // intercepts CTRL-C
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(scheduler));

        scheduler.schedule();

        stopRequestWatcher = StopRequestWatcherImpl.create(settings, scheduler, fileSystem);
        stopRequestWatcher.startWatching();

        scheduler.awaitTermination();
        stopRequestWatcher.stopWatching();
      }
    }

    systemExit.exit(0);
  }

  public static void main(String... args) throws IOException {
    new App().start(args);
  }

  private class ShutdownHook extends Thread {
    private final Scheduler scheduler;

    public ShutdownHook(Scheduler scheduler) {
      super("SonarQube Shutdown Hook");
      this.scheduler = scheduler;
    }

    @Override
    public void run() {
      systemExit.setInShutdownHook();

      if (stopRequestWatcher != null) {
        stopRequestWatcher.stopWatching();
      }

      // blocks until everything is corrected terminated
      scheduler.terminate();
    }
  }
}
