// Autogenerated AST node
package org.python.pydev.parser.jython.ast;

import org.python.pydev.parser.jython.SimpleNode;
import java.util.Arrays;

public final class keywordType extends SimpleNode {
    public NameTokType arg;
    public exprType value;
    public boolean afterstarargs;

    public keywordType(NameTokType arg, exprType value, boolean afterstarargs) {
        this.arg = arg;
        this.value = value;
        this.afterstarargs = afterstarargs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((arg == null) ? 0 : arg.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + (afterstarargs ? 17 : 137);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        keywordType other = (keywordType) obj;
        if (arg == null) {
            if (other.arg != null)
                return false;
        } else if (!arg.equals(other.arg))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (this.afterstarargs != other.afterstarargs)
            return false;
        return true;
    }

    @Override
    public keywordType createCopy() {
        return createCopy(true);
    }

    @Override
    public keywordType createCopy(boolean copyComments) {
        keywordType temp = new keywordType(arg != null ? (NameTokType) arg.createCopy(copyComments) : null,
                value != null ? (exprType) value.createCopy(copyComments) : null, afterstarargs);
        temp.beginLine = this.beginLine;
        temp.beginColumn = this.beginColumn;
        if (this.specialsBefore != null && copyComments) {
            for (Object o : this.specialsBefore) {
                if (o instanceof commentType) {
                    commentType commentType = (commentType) o;
                    temp.getSpecialsBefore().add(commentType.createCopy(copyComments));
                }
            }
        }
        if (this.specialsAfter != null && copyComments) {
            for (Object o : this.specialsAfter) {
                if (o instanceof commentType) {
                    commentType commentType = (commentType) o;
                    temp.getSpecialsAfter().add(commentType.createCopy(copyComments));
                }
            }
        }
        return temp;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("keyword[");
        sb.append("arg=");
        sb.append(dumpThis(this.arg));
        sb.append(", ");
        sb.append("value=");
        sb.append(dumpThis(this.value));
        sb.append(", ");
        sb.append("afterstarargs=");
        sb.append(dumpThis(this.afterstarargs));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Object accept(VisitorIF visitor) throws Exception {
        traverse(visitor);
        return null;
    }

    @Override
    public void traverse(VisitorIF visitor) throws Exception {
        if (arg != null) {
            arg.accept(visitor);
        }
        if (value != null) {
            value.accept(visitor);
        }
    }

}
