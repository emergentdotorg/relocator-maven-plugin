File deps = new File(basedir, 'target/deps.txt')
assert deps.exists()
String text = deps.text
assert text.contains('ch.qos.reload4j:reload4j:jar:1.2.26')
assert !text.contains('log4j:log4j')

String log = new File(basedir, 'build.log').text
assert log.contains('relocator: log4j:log4j:1.2.17 -> ch.qos.reload4j:reload4j:1.2.26')
