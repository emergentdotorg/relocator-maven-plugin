File deps = new File(basedir, 'target/deps.txt')
assert deps.exists()
String text = deps.text
assert text.contains('log4j:log4j:jar:1.2.17')
assert !text.contains('reload4j')
assert text.contains('commons-io:commons-io:jar:2.16.1')
assert !text.contains('commons-io:commons-io:jar:2.11.0')

String log = new File(basedir, 'build.log').text
assert log.contains('relocator: commons-io:commons-io:2.11.0 -> commons-io:commons-io:2.16.1')
