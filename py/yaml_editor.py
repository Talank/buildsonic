import sys
from ruamel.yaml import YAML
inp = """\
# example
name:
  # details
  family: Smith   # very common
  given: Alice    # one of the siblings
"""

yaml = YAML()
code = yaml.load(inp)
code['name']['given'] = 'Bob'

yaml.dump(code, sys.stdout)

filePath = '/Users/zhangchen/projects/BuildPerformance/src/test/resources/travis/unfoldingWord-dev@ts-android.yml'
yaml.preserve_quotes = True
code = yaml.load(open(filePath))
print(code['cache']['directories'].lc.line)
print(code['jdk'].lc.line)
code['branches']['except'] = 'Bob'
#with open('output.yaml', 'w') as fp:
#    yaml.dump(code, fp)
