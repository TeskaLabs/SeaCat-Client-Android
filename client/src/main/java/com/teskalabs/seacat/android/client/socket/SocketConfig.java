package com.teskalabs.seacat.android.client.socket;

public class SocketConfig
{
	public enum Domain {
		AF_UNIX('u'), AF_INET('4'), AF_INET6('6');

		private final char value;
		private Domain(char value) {
			this.value = value;
		}

		public char getValue() {
			return value;
		}
	}

	public enum Type {
		SOCK_STREAM('s'), SOCK_DGRAM('d');

		private final char value;
		private Type(char value) {
			this.value = value;
		}

		public char getValue() {
			return value;
		}

	}

}
