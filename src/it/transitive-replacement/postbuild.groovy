File deps = new File(basedir, 'target/deps.txt')
assert deps.exists()
String text = deps.text
assert text.contains('junit:junit:jar:4.13.2')
assert text.contains('org.hamcrest:hamcrest:jar:2.2')
assert !text.contains('hamcrest-core')

String log = new File(basedir, 'build.log').text
assert log.contains('org.hamcrest:hamcrest-core:1.3 -> org.hamcrest:hamcrest:2.2 (transitive via junit:junit)')
