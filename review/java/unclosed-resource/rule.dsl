// generated-from: review/java/unclosed-resource/rule.md
// rule-hash: 1969b0c62efc3c6f8b0812d3127d5c107dc3af291205f46186676e2a4b194267
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unclosed-resource'

def fileType = { t -> t in ['FileInputStream', 'FileOutputStream', 'FileReader', 'FileWriter', 'Socket', 'ServerSocket', 'RandomAccessFile', 'ZipFile', 'JarFile', 'DatagramSocket'] }
def filesCall = { x -> x.name in ['newInputStream', 'newOutputStream', 'newBufferedReader', 'newBufferedWriter', 'lines', 'walk', 'list', 'find', 'newDirectoryStream'] && x.scope.text == 'Files' }

// Ownership passed on is not always visible, so this is a candidate for the AI.
Creation.each { x ->
    only fileType(x.typeName) || filesCall(x) || (x.name == 'getConnection' && x.args.size().isLessThanOrEqualTo(2))
    skip file.isTest
    // Only a resource held in a local variable ...
    only x.assignedToLocal
    // "Do not flag": try-with-resources, and a resource the method closes, returns, stores or hands to another call.
    skip x.inTryResource || x.closedOrHandedOn

    candidate x, "This resource is not closed on every path: use try-with-resources"
}
