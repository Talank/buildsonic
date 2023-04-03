package parser

import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression

class GradleVisitor extends CodeVisitorSupport {
    private int dependenceLineNum = -1;
    private int columnNum = -1;
    private List<GradleDependency> dependencies = new ArrayList<>()

    private Map<String, String> declaredVariables = new HashMap<>()

    //抽取依赖的标志位
    private boolean dependenciesBlockFlag = false
    //抽取include模块的标志位
    private boolean includeFlag = false
    List<String> moduleNames = []
    Integer allprojectsLastLineNumber = 0
    Integer subprojectsLastLineNumber = 0
    //记录是否并行执行测试用例
    List<BinaryExpression> binaryExpressions = []
    @Override
    public void visitMethodCallExpression(MethodCallExpression call )
    {
//        println(call.text)
//        println(call.lineNumber)
//        println(call.lastLineNumber)
        String methodName = call.getMethodAsString()
        if (methodName.equals( "dependencies" )) {
            if( dependenceLineNum == -1 )
            {
                dependenceLineNum = call.getLastLineNumber();
                //println(dependenceLineNum)
            }
            this.dependenciesBlockFlag = true
            super.visitMethodCallExpression( call );
            this.dependenciesBlockFlag = false
        } else if (methodName.equals("include")) {
            this.includeFlag = true
            super.visitMethodCallExpression( call );
            this.includeFlag = false
        } else if (methodName.equals( "allprojects" )) {
            this.allprojectsLastLineNumber = call.lastLineNumber
        } else if (methodName.equals( "subprojects" )) {
            this.subprojectsLastLineNumber = call.lastLineNumber
        } else {
            super.visitMethodCallExpression( call );
        }
    }

    @Override
    public void visitArgumentlistExpression(ArgumentListExpression ale )
    {
        if (dependenciesBlockFlag) {
            List<Expression> expressions = ale.getExpressions();
            if (expressions.size() == 1 && (expressions.get(0) instanceof ConstantExpression || expressions.get(0) instanceof GStringExpression)) {
                String depStr = expressions.get(0).getText();
                List<String> deps = depStr.split(":");
                //println(deps)
                if (deps.size() == 3 && deps[0].size() > 2 && deps[2].size() > 2) {
                    //println(deps[2])
                    //println(declaredVariables)
                    deps[2] = declaredVariables.getOrDefault(deps[2], deps[2])
                    dependencies.add(new GradleDependency(deps[0], deps[1], deps[2]))
                }
            }
        }

        if (this.includeFlag) {
            List<Expression> expressions = ale.getExpressions();
            expressions.each {
                if (it.getClass() == ConstantExpression.class) {
                    this.moduleNames << it.getText()
                }
            }
        }
        super.visitArgumentlistExpression( ale );
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression )
    {
        if( dependenceLineNum != -1 && expression.getLineNumber() == expression.getLastLineNumber() )
        {
            columnNum = expression.getLastColumnNumber();
        }

        super.visitClosureExpression( expression );
    }

    @Override
    public void visitMapExpression(MapExpression expression )
    {
//        println(expression.text)
//        println(expression.lineNumber)
//        println(expression.lastLineNumber)
        List<MapEntryExpression> mapEntryExpressions = expression.getMapEntryExpressions();
        List<String> keys = mapEntryExpressions.collect {it.getKeyExpression().getText()}
        if (keys.contains("group") && keys.contains("name") && keys.contains("version")) {
            Map<String, String> dependenceMap = new HashMap<String, String>();
            for( MapEntryExpression mapEntryExpression : mapEntryExpressions )
            {
                String key = mapEntryExpression.getKeyExpression().getText();
                String value = mapEntryExpression.getValueExpression().getText();
                dependenceMap.put( key, value );
            }
            dependencies.add( new GradleDependency( dependenceMap ) );
        }

        super.visitMapExpression( expression );
    }

    @Override
    void visitBinaryExpression(BinaryExpression expression) {
//        println(expression.getLeftExpression().class)
//        println(expression.getLeftExpression().text)
//        println(expression.operation.text)
//        println(expression.getRightExpression().class)
//        println(expression.getRightExpression().text)

        binaryExpressions.add(expression)

        if (expression.operation.text == "=" && expression.leftExpression instanceof VariableExpression && expression.rightExpression instanceof ConstantExpression) {
            //println(expression.text)
            String key = expression.leftExpression.text
            String value = expression.rightExpression.text
            declaredVariables.put('$' + key, value)
            declaredVariables.put('${' + key + '}', value)
        }
        super.visitBinaryExpression(expression)
    }

    public int getDependenceLineNum()
    {
        return dependenceLineNum;
    }

    public int getColumnNum()
    {
        return columnNum;
    }

    public List<GradleDependency> getDependencies()
    {
        return dependencies;
    }

}
