// Autogenerated AST node
package org.python.pydev.parser.jython.ast;

import org.python.pydev.parser.jython.SimpleNode;
import java.util.Arrays;

public final class Set extends exprType {
    public exprType[] elts;

    public Set(exprType[] elts) {
        this.elts = elts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(elts);
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
        Set other = (Set) obj;
        if (!Arrays.equals(elts, other.elts))
            return false;
        return true;
    }

    @Override
    public Set createCopy() {
        return createCopy(true);
    }

    @Override
    public Set createCopy(boolean copyComments) {
        exprType[] new0;
        if (this.elts != null) {
            new0 = new exprType[this.elts.length];
            for (int i = 0; i < this.elts.length; i++) {
                new0[i] = (exprType) (this.elts[i] != null ? this.elts[i].createCopy(copyComments) : null);
            }
        } else {
            new0 = this.elts;
        }
        Set temp = new Set(new0);
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
        StringBuffer sb = new StringBuffer("Set[");
        sb.append("elts=");
        sb.append(dumpThis(this.elts));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitSet(this);
    }

    @Override
    public void traverse(VisitorIF visitor) throws Exception {
        if (elts != null) {
            for (int i = 0; i < elts.length; i++) {
                if (elts[i] != null) {
                    elts[i].accept(visitor);
                }
            }
        }
    }

}
