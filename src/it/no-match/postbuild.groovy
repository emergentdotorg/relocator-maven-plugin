File deps = new File(basedir, 'target/deps.txt')
assert deps.exists()
String text = deps.text
assert text.contains('commons-io:commons-io:jar:2.11.0')
assert !text.contains('reload4j')
