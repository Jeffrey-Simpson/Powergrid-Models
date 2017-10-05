package gov.pnnl.goss.cim2glm.components;
//	----------------------------------------------------------
//	Copyright (c) 2017, Battelle Memorial Institute
//	All rights reserved.
//	----------------------------------------------------------

// package gov.pnnl.gridlabd.cim;

import org.apache.jena.query.*;
import java.text.DecimalFormat;
import java.util.HashMap;

public class DistSubstation extends DistComponent {
	public static final String szQUERY = 
		"SELECT ?name ?bus ?basev ?nomv ?vmag ?vang ?r1 ?x1 ?r0 ?x0 ?id WHERE {" +
		" ?s r:type c:EnergySource." +
		" ?s c:IdentifiedObject.name ?name." +
  	" ?s c:ConductingEquipment.BaseVoltage ?bv."+
		" ?bv c:BaseVoltage.nominalVoltage ?basev."+
		" ?s c:EnergySource.nominalVoltage ?nomv." + 
		" ?s c:EnergySource.voltageMagnitude ?vmag." + 
		" ?s c:EnergySource.voltageAngle ?vang." + 
		" ?s c:EnergySource.r ?r1." + 
		" ?s c:EnergySource.x ?x1." + 
		" ?s c:EnergySource.r0 ?r0." + 
		" ?s c:EnergySource.x0 ?x0." + 
		" ?t c:Terminal.ConductingEquipment ?s." +
		" bind(strafter(str(?s),\"#_\") as ?id)."+
		" ?t c:Terminal.ConnectivityNode ?cn." + 
		" ?cn c:IdentifiedObject.name ?bus" +
		"}";

	public String id;
	public String name;
	public String bus;
	public double basev;
	public double nomv;
	public double vmag;
	public double vang;
	public double r1;
	public double x1;
	public double r0;
	public double x0;

	public DistSubstation (ResultSet results) {
		if (results.hasNext()) {
			QuerySolution soln = results.next();
			name = SafeName (soln.get("?name").toString());
			id = soln.get("?id").toString();
			bus = SafeName (soln.get("?bus").toString());
			basev = Double.parseDouble (soln.get("?basev").toString());
			nomv = Double.parseDouble (soln.get("?nomv").toString());
			vmag = Double.parseDouble (soln.get("?vmag").toString());
			vang = Double.parseDouble (soln.get("?vang").toString());
			r1 = Double.parseDouble (soln.get("?r1").toString());
			x1 = Double.parseDouble (soln.get("?x1").toString());
			r0 = Double.parseDouble (soln.get("?r0").toString());
			x0 = Double.parseDouble (soln.get("?x0").toString());
		}		
	}

	public String DisplayString() {
		DecimalFormat df = new DecimalFormat("#0.0000");
		StringBuilder buf = new StringBuilder ("");
		buf.append (name + " @ " + bus + " basev=" + df.format(basev) + " nomv=" + df.format(nomv));
		buf.append (" vmag=" + df.format(vmag));
		buf.append (" vang=" + df.format(vang));
		buf.append (" r1=" + df.format(r1));
		buf.append (" x1=" + df.format(x1));
		buf.append (" r0=" + df.format(r0));
		buf.append (" x0=" + df.format(x0));
		return buf.toString();
	}

	public String GetJSONSymbols(HashMap<String,DistCoordinates> map) {
		DistCoordinates pt = map.get("EnergySource:" + name + ":1");

		DecimalFormat df = new DecimalFormat("#0.0");
		StringBuilder buf = new StringBuilder ();

		buf.append ("{\"name\":\"" + name +"\"");
		buf.append (",\"bus\":\"" + bus +"\"");
		buf.append (",\"phases\":\"ABC\"");
		buf.append (",\"nominal_voltage\":" + df.format(nomv / Math.sqrt(3.0)));
		buf.append (",\"x1\":" + Double.toString(pt.x));
		buf.append (",\"y1\":" + Double.toString(pt.y));
		buf.append ("}");
		return buf.toString();
	}

	public String GetDSS() {
		DecimalFormat df = new DecimalFormat("#0.00000");
		StringBuilder buf = new StringBuilder ("new Circuit." + name);

		buf.append (" phases=3 bus1=" + bus + " basev=" + df.format(nomv) + " pu=" + df.format(vmag/nomv) +
								" angle=" + df.format(vang * 180.0 / Math.PI) + " r0=" + df.format(r0) + 
								" x0=" + df.format(x0) + " r1=" + df.format(r1) + " x1=" + df.format(x1));
		buf.append("\n");

		return buf.toString();
	}

	public String GetKey() {
		return name;
	}
}

