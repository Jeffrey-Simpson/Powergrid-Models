//	----------------------------------------------------------
//	Copyright (c) 2017, Battelle Memorial Institute
//	All rights reserved.
//	----------------------------------------------------------

// package gov.pnnl.gridlabd.cim;

import java.io.*;
import org.apache.jena.query.*;
import java.text.DecimalFormat;
import java.util.HashMap;

public class DistCapacitor extends DistComponent {
    static final String szQUERY = "SELECT ?name ?basev ?nomu ?bsection ?bus ?conn ?grnd ?phs"+
			 " ?ctrlenabled ?discrete ?mode ?deadband ?setpoint ?delay ?monclass ?moneq ?monbus ?monphs WHERE {"+
       " ?s r:type c:LinearShuntCompensator."+
       " ?s c:IdentifiedObject.name ?name."+
			 " ?s c:ConductingEquipment.BaseVoltage ?bv."+
			 " ?bv c:BaseVoltage.nominalVoltage ?basev."+
       " ?s c:ShuntCompensator.nomU ?nomu."+
       " ?s c:LinearShuntCompensator.bPerSection ?bsection."+ 
       " ?s c:ShuntCompensator.phaseConnection ?connraw."+
       " 	bind(strafter(str(?connraw),\"PhaseShuntConnectionKind.\") as ?conn)"+
       " ?s c:ShuntCompensator.grounded ?grnd."+
       " OPTIONAL {?scp c:ShuntCompensatorPhase.ShuntCompensator ?s."+
       " 	?scp c:ShuntCompensatorPhase.phase ?phsraw."+
       " 	bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phs) }"+
       " OPTIONAL {?ctl c:RegulatingControl.RegulatingCondEq ?s."+
       " 	?ctl c:RegulatingControl.discrete ?discrete."+
       " 	?ctl c:RegulatingControl.enabled ?ctrlenabled."+
       " 	?ctl c:RegulatingControl.mode ?moderaw."+
       " 		bind(strafter(str(?moderaw),\"RegulatingControlModeKind.\") as ?mode)"+
       " 	?ctl c:RegulatingControl.monitoredPhase ?monraw."+
       " 		bind(strafter(str(?monraw),\"PhaseCode.\") as ?monphs)"+
       " 	?ctl c:RegulatingControl.targetDeadband ?deadband."+
       " 	?ctl c:RegulatingControl.targetValue ?setpoint."+
       "  ?s c:ShuntCompensator.aVRDelay ?delay."+
       " 	?ctl c:RegulatingControl.Terminal ?trm."+
       " 	?trm c:Terminal.ConductingEquipment ?eq."+
       " 	?eq a ?classraw."+
       " 		bind(strafter(str(?classraw),\"cim16#\") as ?monclass)"+
       " 	?eq c:IdentifiedObject.name ?moneq."+
       " 	?trm c:Terminal.ConnectivityNode ?moncn."+
       " 	?moncn c:IdentifiedObject.name ?monbus."+
       "  }" +
       " ?t c:Terminal.ConductingEquipment ?s."+
       " ?t c:Terminal.ConnectivityNode ?cn."+ 
       " ?cn c:IdentifiedObject.name ?bus" + 
       "}";

	public String name;
	public String bus;
	public String phs;
	public String conn;
	public String grnd;
	public String ctrl;
	public double nomu;
	public double basev;
	public double kvar;
	public String mode;
	public double setpoint;
	public double deadband;
	public double delay;
	public String moneq;
	public String monclass;
	public String monbus;
	public String monphs;

	private double kvar_A;
	private double kvar_B;
	private double kvar_C;
	private boolean bDelta;
	private int nphases;

	private void SetDerivedParameters() {
		int bA = 0, bB = 0, bC = 0;
		if (phs.contains ("A")) bA = 1;
		if (phs.contains ("B")) bB = 1;
		if (phs.contains ("C")) bC = 1;
		double kvar_ph = kvar / (bA + bB + bC);
		kvar_A = kvar_ph * bA;
		kvar_B = kvar_ph * bB;
		kvar_C = kvar_ph * bC;
		if (conn.equals("D")) {
			bDelta = true;
		} else {
			bDelta = false;
		}
		nphases = bA + bB + bC;
	}

	/** translate the capacitor control mode from CIM to GridLAB-D
	 *  @param s CIM regulating control mode enum
	 *  @return MANUAL, CURRENT, VOLT, VAR */
	private String GLMCapMode (String s) {
		if (s.equals("currentFlow")) return "CURRENT";
		if (s.equals("voltage")) return "VOLT";
		if (s.equals("reactivePower")) return "VAR";
		if (s.equals("timeScheduled")) return "MANUAL";  // TODO - support in GridLAB-D?
		if (s.equals("powerFactor")) return "MANUAL";  // TODO - support in GridLAB-D?
		if (s.equals("userDefined")) return "MANUAL"; 
		return "time";
	}

	public DistCapacitor (ResultSet results) {
		if (results.hasNext()) {
			QuerySolution soln = results.next();
			name = GLD_Name (soln.get("?name").toString(), false);
			bus = GLD_Name (soln.get("?bus").toString(), true);
			basev = Double.parseDouble (soln.get("?basev").toString());
			phs = OptionalString (soln, "?phs", "ABC");
			conn = soln.get("?conn").toString();
			grnd = soln.get("?grnd").toString();
			ctrl = OptionalString (soln, "?ctrlenabled", "false");
			nomu = Double.parseDouble (soln.get("?nomu").toString());
			double bsection = Double.parseDouble (soln.get("?bsection").toString());
			kvar = nomu * nomu * bsection / 1000.0;
			if (ctrl.equals ("true")) {
				mode = soln.get("?mode").toString();
				setpoint = Double.parseDouble (soln.get("?setpoint").toString());
				deadband = Double.parseDouble (soln.get("?deadband").toString());
				delay = Double.parseDouble (soln.get("?delay").toString());
				moneq = soln.get("?moneq").toString();
				monclass = soln.get("?monclass").toString();
				monbus = soln.get("?monbus").toString();
				monphs = soln.get("?monphs").toString();
			}
			SetDerivedParameters();
		}
	}

	public String DisplayString() {
		DecimalFormat df = new DecimalFormat("#0.0000");
		StringBuilder buf = new StringBuilder ("");
		buf.append (name + " @ " + bus + " on " + phs + " basev=" + df.format(basev));
		buf.append (" " + df.format(nomu/1000.0) + " [kV] " + df.format(kvar) + " [kvar] " + "conn=" + conn + " grnd=" + grnd);
		if (ctrl.equals ("true")) {
			buf.append("\n	control mode=" + mode + " set=" + df.format(setpoint) + " bandwidth=" + df.format(deadband) + " delay=" + df.format(delay));
			buf.append(" monitoring: " + moneq + ":" + monclass + ":" + monbus + ":" + monphs);
		}
		return buf.toString();
	}

	public String GetJSONSymbols(HashMap<String,DistCoordinates> map) {
		DistCoordinates pt = map.get("LinearShuntCompensator:" + name + ":1");

		DecimalFormat df = new DecimalFormat("#0.0");
		StringBuilder buf = new StringBuilder ();

		buf.append ("{\"name\":\"" + name +"\"");
		buf.append (",\"parent\":\"" + bus +"\"");
		buf.append (",\"phases\":\"" + phs +"\"");
		buf.append (",\"kvar_A\":" + df.format(kvar_A));
		buf.append (",\"kvar_B\":" + df.format(kvar_B));
		buf.append (",\"kvar_C\":" + df.format(kvar_C));
		buf.append (",\"x1\":" + Double.toString(pt.x));
		buf.append (",\"y1\":" + Double.toString(pt.y));
		buf.append ("}");
		return buf.toString();
	}

	public String GetGLM() {
		StringBuilder buf = new StringBuilder ("object capacitor {\n");
		DecimalFormat df = new DecimalFormat("#0.00");

		buf.append ("  name \"cap_" + name + "\";\n");
		buf.append ("  parent \"" + bus + "\";\n");
		if (bDelta) {
			buf.append ("  phases " + phs + "D;\n");
			buf.append ("  phases_connected " + phs + "D;\n");
		} else {
			buf.append ("  phases " + phs + "N;\n");
			buf.append ("  phases_connected " + phs + "N;\n");
		}
		double gld_nomu = nomu;
		if (nphases > 1) {
			gld_nomu /= Math.sqrt(3.0);
		}
		buf.append("  cap_nominal_voltage " + df.format(gld_nomu) + ";\n");
		if (kvar_A > 0.0) {
			buf.append ("  capacitor_A " + df.format(kvar_A * 1000.0) + ";\n");
			buf.append ("  switchA CLOSED;\n");
		}
		if (kvar_B > 0.0) {
			buf.append ("  capacitor_B " + df.format(kvar_B * 1000.0) + ";\n");
			buf.append ("  switchB CLOSED;\n");
		}
		if (kvar_C > 0.0) {
			buf.append ("  capacitor_C " + df.format(kvar_C * 1000.0) + ";\n");
			buf.append ("  switchC CLOSED;\n");
		}
		if (ctrl.equals("true")) {
			String glmMode = GLMCapMode (mode);
			double dOn = setpoint - 0.5 * deadband;
			double dOff = setpoint + 0.5 * deadband;
			buf.append ("  control MANUAL; // " + glmMode + ";\n");
			if (glmMode.equals("VOLT"))  {
				buf.append ("  voltage_set_low " + df.format(dOn) + ";\n");
				buf.append ("  voltage_set_high " + df.format(dOff) + ";\n");
			} else if (glmMode.equals("CURRENT"))  {
				buf.append ("  current_set_low " + df.format(dOn) + ";\n");
				buf.append ("  current_set_high " + df.format(dOff) + ";\n");
			} else if (glmMode.equals("VAR"))  {
				// in GridLAB-D, positive VAR flow is from capacitor into the upstream remote sensing link (opposite of OpenDSS)
				buf.append ("  VAr_set_low " + df.format(dOff) + ";\n");
				buf.append ("  VAr_set_high " + df.format(dOn) + ";\n");
			} else if (mode.equals("timeScheduled")) {
				buf.append ("  // CIM timeScheduled on=" + df.format(dOn) + " off=" + df.format(dOff) + ";\n");
			}
			String glmClass = GLMClassPrefix(monclass);
			if (!glmClass.equals("cap") || !moneq.equals(name)) {
				buf.append("	remote_sense \"" + glmClass + "_" + moneq + "\";\n");
			}
			buf.append ("  pt_phase " + monphs + ";\n");
			if (monphs.length() > 1) {
				buf.append("	control_level INDIVIDUAL;\n");
			} else {
				buf.append("	control_level BANK;\n");
			}
			buf.append ("  dwell_time " + df.format(delay) + ";\n");
		}
		buf.append("}\n");

		return buf.toString();
	}

	public String GetKey() {
		return name;
	}
}

