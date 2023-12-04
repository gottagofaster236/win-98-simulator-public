package com.lr_soft.windows98simulator.Applications;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Window;

import java.util.ArrayList;
import java.util.List;

public class Calculator extends Window {
    private String currentText = "0";
    private String currentAction = null;  // / * + -, то есть операции от 2 переменных
    private String lastAction = null;  // если мы будем много раз нажимать кнопку "="
    private double lastOperand = 0;
    private double currentResult = 0;  // возможно промежуточный, появляющийся после нажатия "+"
    private boolean isShowingArtificial = false;  // показываем что-то сгенерированное машиной
    private boolean isShowingResultAfterEquals = false;  // после нажатия кнопки "равно". В этом случае мы после нажатия цифровой кнопки сбрасываемся всё полностью
    private boolean error = false;
    private boolean lastClickOnSign = false;
    private double memory = 0;

    private static String toString(double number){
        String returnValue = String.valueOf(number).replaceAll(",", ".").toLowerCase()
                .replaceAll("e", "e+");
        if(returnValue.endsWith(".0"))
            returnValue = returnValue.substring(0, returnValue.length() - 2);
        return returnValue;
    }

    public Calculator(){
        super("Calculator", getBmp(R.drawable.calculator_1),
                260, 252, false, true, false);
        unableToMaximize = true;
        // top menu
        ButtonList edit = new ButtonList();
        edit.elements.add(new ButtonInList("Copy", "Ctrl+C"));
        ButtonInList paste = new ButtonInList("Paste", "Ctrl+V");
        paste.disabled = true;
        edit.elements.add(paste);
        List<ButtonInList> viewGroup = new ArrayList<>();
        viewGroup.add(new ButtonInList("Standard"));
        viewGroup.add(new ButtonInList("Scientific"));
        ButtonList view = new ButtonList();
        viewGroup.get(0).checkActive = true;
        for(ButtonInList b : viewGroup){
            b.radioButtonGroup = viewGroup;
            view.elements.add(b);
        }
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> new HelpTopics("Calculator Help", true,
                new int[]{R.drawable.calc1, R.drawable.calc2, R.drawable.calc3, R.drawable.calc4});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About Calculator");
        about.action = parent -> new AboutWindow(Calculator.this, "Calculator", getBmp(R.drawable.calculator_0));
        help.elements.add(about);
        topMenu.elements.add(new TopMenuButton("Edit", edit));
        topMenu.elements.add(new TopMenuButton("View", view));
        topMenu.elements.add(new TopMenuButton("Help", help));
        // buttons
        addElement(new Button("Backspace", 63, 29, 57, 78, Color.RED, parent -> {
            if(error)
                return;
            if(isShowingArtificial)
                return;
            currentText = currentText.substring(0, currentText.length() - 1);
            if(currentText.isEmpty() || currentText.equals("-"))
                currentText = "0";
        }));
        addElement(new Button("CE", 63, 29, 123, 78, Color.RED, parent -> {
            currentText = "0";
            lastClickOnSign = false;
            if(error){
                error = false;
                currentResult = 0;
                isShowingArtificial = false;
                isShowingResultAfterEquals = false;
                currentAction = null;
                lastAction = null;
            }
        }));
        addElement(new Button("C", 63, 29, 188, 78, Color.RED, parent -> {
            currentText = "0";
            error = false;
            currentResult = 0;
            isShowingArtificial = false;
            isShowingResultAfterEquals = false;
            currentAction = null;
            lastClickOnSign = false;
            lastAction = null;
        }));
        OnClickRunnable digitAction = parent -> {
            if(error)
                return;
            lastClickOnSign = false;
            if(isShowingArtificial){
                isShowingArtificial = false;
                currentText = "0";
                if(isShowingResultAfterEquals){  // тогда сбрасываем всё полностью
                    currentResult = 0;
                    isShowingArtificial = false;
                    isShowingResultAfterEquals = false;
                    currentAction = null;
                    lastAction = null;
                }
            }
            String digit = ((Button) parent).text;
            if(currentText.length() == 32)
                return;
            if(digit.equals(".")){
                if(!currentText.contains("."))
                    currentText += ".";
                return;
            }
            if(currentText.equals("0") && digit.equals("0"))
                return;
            if(currentText.equals("0"))
                currentText = "";
            currentText += digit;
        };
        for(int i = 0; i <= 3; i++){
            for(int j = 0; j < 3; j++){
                String buttonText = String.valueOf(7 - i * 3 + j);
                if(i == 3)
                    buttonText = "0";
                addElement(new Button(buttonText, 36, 29, 57 + 39 * j, 114 + 33 * i, Color.BLUE, digitAction));
                if(i == 3)  // только одна кнопка в 4-м ряду, это ноль
                    break;
            }
        }
        addElement(new Button(".", 36, 29, 135, 212, Color.BLUE, digitAction));
        OnClickRunnable twoOperandAction = parent -> {
            lastAction = null;
            if (error)
                return;
            if (!lastClickOnSign)
                Calculator.this.equals();
            lastClickOnSign = true;
            currentAction = ((Button) parent).text;
            isShowingArtificial = true;
        };
        addElement(new Button("/", 36, 29, 174, 114, Color.RED, twoOperandAction));
        addElement(new Button("*", 36, 29, 174, 147, Color.RED, twoOperandAction));
        addElement(new Button("-", 36, 29, 174, 179, Color.RED, twoOperandAction));
        addElement(new Button("+", 36, 29, 174, 212, Color.RED, twoOperandAction));
        addElement(new Button("=", 36, 29, 213, 212, Color.RED, parent -> {
            if(error)
                return;
            Calculator.this.equals();
            lastClickOnSign = false;
            isShowingArtificial = true;
            isShowingResultAfterEquals = true;
        }));
        // singleOperandActions меняют то, что написано в currentString
        OnClickRunnable singleOperandAction = parent -> {
            if(error)
                return;
            lastClickOnSign = false;
            double cur;
            try{
                cur = Double.parseDouble(currentText);
            }
            catch (Exception e){
                error = true;
                return;
            }
            isShowingArtificial = true;
            switch(((Button) parent).text){
                case "+/-":
                    cur = -cur;
                    break;
                case "1/x":
                    if(cur == 0){
                        error = true;
                        currentText = "Error: Positive Infinity";
                        return;
                    }
                    cur = 1 / cur;
                    break;
                case "sqrt":
                    if(cur < 0){
                        error = true;
                        currentText = "Invalid input for function";
                        return;
                    }
                    cur = Math.sqrt(cur);
                    break;
            }
            currentText = toString(cur);
        };
        addElement(new Button("sqrt", 36, 29, 213, 114, Color.BLUE, singleOperandAction));
        addElement(new Button("1/x", 36, 29, 213, 179, Color.BLUE, singleOperandAction));
        addElement(new Button("+/-", 36, 29, 96, 212, Color.BLUE, singleOperandAction));
        addElement(new Button("%", 36, 29, 213, 147, Color.BLUE, parent -> percent()));
        // Память
        addElement(new Button("MC", 36, 29, 11, 114, Color.RED, parent -> {
            if(error)
                return;
            memory = 0;
        }));
        addElement(new Button("MR", 36, 29, 11, 147, Color.RED, parent -> {
            if(error)
                return;
            currentText = toString(memory);
        }));
        addElement(new Button("MS", 36, 29, 11, 179, Color.RED, parent -> {
            if(error)
                return;
            try {
                memory = Double.parseDouble(currentText);
            }
            catch (Exception e) {
                error = true;
            }
        }));
        addElement(new Button("M+", 36, 29, 11, 212, Color.RED, parent -> {
            if(error)
                return;
            try {
                memory += Double.parseDouble(currentText);
            }
            catch (Exception e) {
                error = true;
            }
        }));
        restore();
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        // рисуем поле ввода
        drawFrameRectActive(canvas, x + 11, y + 41, x + 250, y + 67);
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 13, y + 43, x + 248, y + 65, p);
        p.setColor(Color.BLACK);
        p.setTextAlign(Paint.Align.RIGHT);
        String drawText = currentText;
        if(!currentText.contains("."))
            drawText += ".";
        canvas.drawText(drawText, x + 246, y + 58, p);
        p.setTextAlign(Paint.Align.LEFT);
        // рисуем квадратик с буквой M (память)
        drawFrameRectActive(canvas, x + 15, y + 80, x + 42, y + 106);
        if(memory != 0){
            p.setColor(Color.BLACK);
            canvas.drawText("M", x + 26, y + 97, p);
        }
    }

    private void equals(){  // если нажимаем на кнопку "="
        double secondOperand = 0;
        boolean setSecondOperand = false;  // возможно, мы восстановили операнд по предыдущим действиям (несколько раз нажимали "=")
        if(currentAction == null){
            currentAction = lastAction;
            secondOperand = lastOperand;
            if(currentAction != null)
                setSecondOperand = true;
        }
        if(currentAction == null) {
            isShowingArtificial = true;
            isShowingResultAfterEquals = false;
            try {
                currentResult = Double.parseDouble(currentText);
            }
            catch (Exception e) {
                error = true;
                return;
            }
            return;
        }
        if(!setSecondOperand) {
            try {
                secondOperand = Double.parseDouble(currentText);
            }
            catch (Exception e) {
                error = true;
                return;
            }
        }
        double result = 0;  // чтобы компилятор не ругался
        switch(currentAction){
            case "+":
                result = currentResult + secondOperand;
                break;
            case "-":
                result = currentResult - secondOperand;
                break;
            case "/":
                if(secondOperand == 0){
                    error = true;
                    currentText = "Error: Positive Infinity";
                    return;
                }
                result = currentResult / secondOperand;
                break;
            case "*":
                result = currentResult * secondOperand;
                break;
        }
        currentText = toString(result);
        currentResult = result;
        isShowingArtificial = true;
        isShowingResultAfterEquals = false;

        lastAction = currentAction;
        lastOperand = secondOperand;
        currentAction = null;
    }

    private void percent() {  // если нажимаем на кнопку "%". Она просто изменяет текст
        if (error)
            return;
        lastClickOnSign = false;
        double cur;
        try {
            cur = Double.parseDouble(currentText);
        }
        catch (Exception e){
            error = true;
            return;
        }
        if (currentAction == null)
            cur = 0;
        else if(currentAction.equals("+") || currentAction.equals("-")) {  // плюс или минус
            cur = currentResult * cur / 100;
        }
        else if(currentAction.equals("*") || currentAction.equals("/")){
            cur /= 100;
        }
        currentText = Calculator.toString(cur);
    }

    @Override
    public void onKeyPress(String key) {
        if(key.equals("C"))
            return;
        for(Element element : elements){
            if(!(element instanceof Button))
                continue;
            Button b = (Button) element;
            if(key.equalsIgnoreCase(b.text)
                    || (key.equals("DEL") && b.text.equals("Backspace"))
                    || (key.equals("\n") && b.text.equals("="))) {
                b.action.run(b);
                return;
            }
        }
    }
}
