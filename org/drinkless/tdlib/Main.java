package org.drinkless.tdlib;

import org.drinkless.tdlib.Auth.Authentication;

import java.io.IOException;

public class Main {
	public static void main(String args[]) throws InterruptedException, IOException {
		Authentication myApp = new Authentication();
		myApp.loginTelegram();
	}
}
