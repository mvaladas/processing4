package processing.mode.java.lsp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.lsp4j.Location;

import processing.app.SketchCode;
import processing.mode.java.PreprocSketch;
import processing.mode.java.SketchInterval;

import static processing.mode.java.ASTUtils.getSimpleNameAt;
import static processing.mode.java.ASTUtils.resolveBinding;


public class PdeSymbolFinder {
  
  /**
   * searches a declaration node for a provided character offset
   *
   * @param ps         processed sketch, for AST-nodes and sketch
   * @param javaOffset character offset for the node we want to look up
   * @return Location list if a declaration is found, else an empty list.
   */
  static public List<? extends Location> searchDeclaration(PreprocSketch ps, int javaOffset) {
    ASTNode root = ps.compilationUnit;
    
    SimpleName simpleName = getSimpleNameAt(root, javaOffset, javaOffset);
    if (simpleName == null) {
      System.out.println("no simple name found at location");
      return Collections.emptyList();
    }
    
    IBinding binding = resolveBinding(simpleName);
    if (binding == null) {
      System.out.println("binding not resolved");
      return Collections.emptyList();
    }
    
    String key = binding.getKey();
    ASTNode declarationNode = ps.compilationUnit.findDeclaringNode(key);
    if (declarationNode == null) {
      System.out.println("declaration not found");
      return Collections.emptyList();
    }
    
    SimpleName declarationName = switch (binding.getKind()) {
      case IBinding.TYPE -> ((TypeDeclaration) declarationNode).getName();
      case IBinding.METHOD -> ((MethodDeclaration) declarationNode).getName();
      case IBinding.VARIABLE ->
        ((VariableDeclaration) declarationNode).getName();
      default -> null;
    };
    
    if (declarationName == null) {
      System.out.println("declaration name not found " + declarationNode);
      return Collections.emptyList();
    }
    
    if (!declarationName.equals(simpleName)) {
      System.out.println("found declaration, name: " + declarationName);
    } else {
      System.out.println("already at declaration");
    }
    
    SketchInterval si = ps.mapJavaToSketch(declarationName);
    if (si == SketchInterval.BEFORE_START) {
      System.out.println("declaration is outside of the sketch");
      return Collections.emptyList();
    }
    
    //Create a location for the found declaration
    SketchCode code = ps.sketch.getCode(si.tabIndex);
    String program = code.getProgram();
    URI uri = PdeAdapter.pathToUri(code.getFile());
    
    Location location =
      PdeAdapter.toLocation(program, si.startTabOffset, si.stopTabOffset, uri);
    
    List<Location> declarationList = new ArrayList<>();
    declarationList.add(location);
    
    return declarationList;
  }
}
