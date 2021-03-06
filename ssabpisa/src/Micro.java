/*
    This program is part of an assignment for ECE468 at Purdue University, IN.
    Copying, modifying or reusing this program may result in disciplinary actions.
    
    Copyright (C) 2014-2075 S. Sabpisal

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Micro{
  public static int CONST_NUM_REG_USE = 4; 
  public static boolean DATAFLOW_VERBOSE = false;
  public static boolean TINYGEN_VERBOSE = false;
  
  public static void main(String[] args) throws Exception
  {

      /*
      Loop though multiple arguments source file
      to sample code coverage
       */
    boolean print_out = true;
	for(String arg : args){

        CharStream in = new ANTLRFileStream(arg);
        MicroLexer lexer = new MicroLexer(in);
        CommonTokenStream tks = new CommonTokenStream(lexer);
        MicroParser psr = new MicroParser(tks);
        ParseTreeWalker walker = new ParseTreeWalker();
        ExtractionListener extractor = new ExtractionListener(psr);
        psr.setErrorHandler(new BailErrorStrategy());
        ParseTree t = null;

        try{
            t = psr.program();
        } catch (Exception fpe) {
            fpe.printStackTrace();
            System.out.println("Not accepted");
            System.exit(1);
        }

        walker.walk(extractor, t);

        System.out.println(";IR code");

        DataflowBuilder cfgb = new DataflowBuilder(CONST_NUM_REG_USE);
        cfgb.setMode(cfgb.BOTTOM_UP);
        cfgb.setGlobalVars(extractor.root_scope);

        for(String fn : extractor.getFullIR().keySet()){
            IRList block = extractor.getFullIR().get(fn);
            Utils.printIR(cfgb.enforce(block));
        }

        System.out.println(";----------------- tiny ------------------------");

        //Generate Tiny Code
        StringBuffer tiny_buffer = new StringBuffer();
        //Do global scope declarations
        for(Id symbol: extractor.root_scope){

            String s =  String.format("var %s\n", symbol.getReferenceName());
            if(symbol.getType() == "STRING"){
                s = String.format("str %s %s\n", symbol.getReferenceName(), symbol.getStrValue());
            }
            tiny_buffer.append(s);
        }
        //Do push registers and JSR main and halt
        tiny_buffer.append(ISA.push.getName() + "\n");
        for(int i = 0; i< Micro.CONST_NUM_REG_USE; i++){
            tiny_buffer.append(String.format("%s r%d\n", ISA.push.getName(), i));
        }
        tiny_buffer.append(ISA.jsr.getName() + " main\n");
        tiny_buffer.append("sys halt\n");


        //Generate the rest of the tiny
        for(String fn : extractor.getFullIR().keySet()){
            //Utils.printIR(extractor.getFullIR().get(fn));

            TinyGenerator asmgen = new TinyGenerator(extractor.getFullIR().get(fn), extractor.getSymbolTableMap());
            tiny_buffer.append(asmgen.translate());
        }


       if(print_out) System.out.println(tiny_buffer);
        print_out = false;
    }


  }
}


