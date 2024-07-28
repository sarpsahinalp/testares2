package de.tum.cit.ase.ares.integration.testuser.subject.pathaccess;

import ch.qos.logback.classic.log4j.XMLLayout;

import java.io.*;
import java.nio.file.*;

public final class PathAccessPenguin {

	private PathAccessPenguin() {
	}

	public static void accessPath(Path p) throws IOException {
		Files.readString(p);
	}

	public static void askForFilePermission(String path) {
		//REMOVED: Checking Permission of the system's SecurityManager for "read"
	}
}
