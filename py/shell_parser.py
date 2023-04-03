from tree_sitter import Language, Parser
import os
import sys
import json
# Language.build_library(
#     # Store the library in the `build` directory
#     'build/my-languages.so',
#
#     # Include one or more languages
#     [
#         'vendor/tree-sitter-bash',
#     ]
# )
class ShellParser():

    def __init__(self, shellPath):
        self.declaredVariableMap = {}
        self.commands = []

        dirname = os.path.dirname(__file__)
        soFilePath = os.path.abspath(os.path.join(dirname, 'build', 'languages.so'))
        BASH_LANGUAGE = Language(soFilePath, 'bash')
        parser = Parser()
        parser.set_language(BASH_LANGUAGE)
        with open(shellPath) as f:
            s = f.read()
        self.byteAry = bytes(s, "utf8")
        tree = parser.parse(self.byteAry)
        self.traverse(tree.root_node)

    def print_node(self, node):
        print(node.type)
        print(node.sexp())
        print(self.byteAry[node.start_byte:node.end_byte].decode('utf8'))

    def parseVariableAssignment(self, statement):
        key = None
        keyBrace = None
        value = None
        for child in statement.children:
            if child.type == 'variable_name' or child.type == 'subscript':
                key = '$' + self.byteAry[child.start_byte:child.end_byte].decode('utf8')
                keyBrace = '${' + self.byteAry[child.start_byte:child.end_byte].decode('utf8') + '}'
            elif child.type == 'string':
                value = self.byteAry[child.start_byte:child.end_byte].decode('utf8')[1:-2]
        if value is not None:
            self.declaredVariableMap[key] = value
            self.declaredVariableMap[keyBrace] = value
        #print(self.declaredVariableMap)

    def parseCommand(self, statement):
        command = self.byteAry[statement.start_byte:statement.end_byte].decode('utf8')
        for key, value in self.declaredVariableMap.items():
            if key in command:
                command = command.replace(key, value)
        #print(command)
        if 'mvn' in command or 'gradle' in command:
            map = {
                "command": command,
                "startLineNumber": statement.start_point[0],
                "endLineNumber": statement.end_point[0],
                "startColumnNumber": statement.start_point[1],
                "endColumnNumber": statement.end_point[1],
                "startByte": statement.start_byte,
                "endByte": statement.end_byte,
                "command": command
            }
            self.commands.append(map)

    def traverse(self, node):
        #self.print_node(node)
        if node.type == 'variable_assignment':
            self.parseVariableAssignment(node)
        elif node.type == 'command':
            self.parseCommand(node)
        else:
            for child in node.children:
                self.traverse(child)

#'/home/fdse/user/zc/sequence/repositor2y/Noahs-ARK@semafor/bin/tokenizeAndPosTag.sh'
REPO_DIR_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'sequence', 'repository'))
JSON_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'resources', 'shellCommandJson'))

def commandsToMap(filePath, repoName, relativeFilePath):
    list = []
    parser = None
    try:
        parser = ShellParser(filePath)
    except Exception as e:
        print(filePath)
        print(e)
    if parser is None:
        return list
    commands = parser.commands
    for command in commands:
        map = {
            "repoName": repoName,
            "filePath": relativeFilePath,
            "command": command
        }
        list.append(map)
    return list

def store(filePaths):
    result = []
    repoName = None
    for filePath in filePaths:
        relativePath = os.path.relpath(filePath, REPO_DIR_PATH)
        parts = relativePath.split(os.path.sep)
        repoName = parts[0]
        relativeFilePath = os.path.join(*parts[1:])
        result = result + commandsToMap(filePath, repoName, relativeFilePath)

    if len(result) <=0:
        return
    storePath = os.path.join(JSON_DIR, repoName + '.json')
    #print(storePath)
    with open(storePath, "w") as f:
        json.dump(result, f, indent=4)

if __name__ == '__main__':
    #print(str(sys.argv))
    filePaths = sys.argv[1:]
    #print(filePaths)
    #filePaths = [ filePath[1:-1] for filePath in filePaths]
    # for filePath in filePaths:
    #     print(filePath)
    store(filePaths)

