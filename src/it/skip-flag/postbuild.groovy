File deps = new File(basedir, 'target/deps.txt')
assert deps.exists()
String text = deps.text
assert text.contains('log4j:log4j:jar:1.2.17')
assert !text.contains('reload4j')

String log = new File(basedir, 'build.log').text
assert log.contains('relocator: dependency replacement is skipped')
