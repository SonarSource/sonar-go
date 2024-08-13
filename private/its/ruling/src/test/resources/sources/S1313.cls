public class Foo {

    static String IP_ADDRESS = '78.78.78.78';
    static String NOT_IP_ADDRESS = '10.10.10.256';

    private String nonCompliant() {
      String ip1 = '192.168.12.42';
      String ip2 = '10.10.10.10';
      String ip3 = '192.168.12.42';
      String ipPort = '192.168.12.42:80';
      String ipV6_1 = '2001:db8:85a3:0:0:8a2e:370:7334';
      String ipV6_2 = '2001:db8:85a3::8a2e:370:7334';
      foo('8.8.8.8');
      return '1.2.3.4';
    }

    private String compliant() {
      String ip1 = '127.0.0.1';
      String ip2 = '127.255.0.1';
      String ip3 = '255.255.255.255';
      String notAnIp1 = '192.168.12';
      String notAnIp2 = '1234.0.0.0.0.1234';
      String notAnIp3 = '192.168.12.42-1';
      String notAnIp4 = 'hello';
      String ipV6_ok1 = '000:0::0';
      String ipV6_ok2 = '000:0::1';
      foo('0.0.0.0');
      return '2.5.5.5';
    }

    private void foo(String s) {}
}
