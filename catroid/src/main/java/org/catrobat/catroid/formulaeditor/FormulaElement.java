package org.catrobat.catroid.formulaeditor;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.FloatArrayManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.TableManager;
import org.catrobat.catroid.content.UserVarsManager;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.fast2d.Fast2DFormulaBridge;
import org.catrobat.catroid.fast2d.FastTwoDManager;
import org.catrobat.catroid.formulaeditor.common.Conversions;
import org.catrobat.catroid.formulaeditor.function.ArduinoFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.BinaryFunction;
import org.catrobat.catroid.formulaeditor.function.FormulaFunction;
import org.catrobat.catroid.formulaeditor.function.FunctionProvider;
import org.catrobat.catroid.formulaeditor.function.MathFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.ObjectDetectorFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.RaspiFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.TernaryFunction;
import org.catrobat.catroid.formulaeditor.function.TextBlockFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.TouchFunctionProvider;
import org.catrobat.catroid.libraries.LibraryManager;
import org.catrobat.catroid.libraries.LoadedLibrary;
import org.catrobat.catroid.ml.MLBridge;
import org.catrobat.catroid.physics.PhysicsWorld;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.sensing.CollisionDetection;
import org.catrobat.catroid.sensing.ColorAtXYDetection;
import org.catrobat.catroid.sensing.ColorCollisionDetection;
import org.catrobat.catroid.sensing.ColorEqualsColor;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;
import org.catrobat.catroid.utils.ChEngineMqttManager;
import org.catrobat.catroid.utils.ChEngineNotificationManager;
import org.catrobat.catroid.utils.lunoscript.Interpreter;
import org.catrobat.catroid.utils.lunoscript.LunoRuntimeError;
import org.catrobat.catroid.utils.lunoscript.LunoValue;
import org.catrobat.catroid.utils.lunoscript.Token;
import org.catrobat.catroid.utils.lunoscript.TokenType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

import static org.catrobat.catroid.formulaeditor.InternTokenType.BRACKET_CLOSE;
import static org.catrobat.catroid.formulaeditor.InternTokenType.BRACKET_OPEN;
import static org.catrobat.catroid.formulaeditor.InternTokenType.COLLISION_FORMULA;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_NAME;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_PARAMETERS_BRACKET_CLOSE;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_PARAMETERS_BRACKET_OPEN;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_PARAMETER_DELIMITER;
import static org.catrobat.catroid.formulaeditor.InternTokenType.NUMBER;
import static org.catrobat.catroid.formulaeditor.InternTokenType.OPERATOR;
import static org.catrobat.catroid.formulaeditor.InternTokenType.SENSOR;
import static org.catrobat.catroid.formulaeditor.InternTokenType.STRING;
import static org.catrobat.catroid.formulaeditor.InternTokenType.USER_DEFINED_BRICK_INPUT;
import static org.catrobat.catroid.formulaeditor.InternTokenType.USER_LIST;
import static org.catrobat.catroid.formulaeditor.InternTokenType.USER_VARIABLE;
import static org.catrobat.catroid.formulaeditor.common.Conversions.FALSE;
import static org.catrobat.catroid.formulaeditor.common.Conversions.TRUE;
import static org.catrobat.catroid.formulaeditor.common.Conversions.booleanToDouble;
import static org.catrobat.catroid.formulaeditor.common.Conversions.convertArgumentToDouble;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretOperatorEqual;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretSensor;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretUserDefinedBrickInput;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretUserList;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretUserVariable;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.isInteger;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.normalizeDegeneratedDoubleValues;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryInterpretCollision;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryInterpretDoubleValue;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryInterpretElementRecursive;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryParseIntFromObject;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementResources.addFunctionResources;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementResources.addSensorsResources;
import static org.catrobat.catroid.utils.NumberFormats.trimTrailingCharacters;

import org.mozilla.javascript.ContextFactory;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.danvexteam.lunoscript_annotations.LunoClass;

@LunoClass
public class FormulaElement implements Serializable {

    private static final long serialVersionUID = 1L;
    private static boolean rhinoInitialized = false;
    private static final String TAG_FORMULA_ELEMENT = "FormulaElement";

    private transient org.catrobat.catroid.formulaeditor.UserVariable cachedUserVariable = null;
    private transient org.catrobat.catroid.formulaeditor.UserList cachedUserList = null;
    private transient org.catrobat.catroid.content.Scope cachedScope = null;

    private transient org.catrobat.catroid.formulaeditor.UserData cachedBrickInput = null;
    private transient com.badlogic.gdx.scenes.scene2d.actions.SequenceAction cachedSequence = null;

    private transient Double cachedDoubleValue = null;

    private static org.luaj.vm2.Globals luaGlobals = null;
    private static final java.util.Map<String, org.luaj.vm2.LuaValue> compiledLuaCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int MAX_CACHED_REGEX_PATTERNS = 200;
    private static final java.util.Map<String, Pattern> compiledRegexCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public enum ElementType {
        OPERATOR, FUNCTION, NUMBER, SENSOR, USER_VARIABLE, USER_LIST, USER_DEFINED_BRICK_INPUT, BRACKET, STRING, COLLISION_FORMULA
    }

    private static void ensureRhinoInitialized() {
        if (!ContextFactory.hasExplicitGlobal()) {
            ContextFactory.initGlobal(new ContextFactory());
        }
    }

    public ElementType type;
    private String value;
    public FormulaElement leftChild = null;
    public FormulaElement rightChild = null;
    public List<FormulaElement> additionalChildren;
    private transient FormulaElement parent;
    private transient Map<Functions, FormulaFunction> formulaFunctions;
    private transient TextBlockFunctionProvider textBlockFunctionProvider;

    protected FormulaElement() {
        textBlockFunctionProvider = new TextBlockFunctionProvider();
        List<FunctionProvider> functionProviders = Arrays.asList(
                new ArduinoFunctionProvider(),
                new RaspiFunctionProvider(),
                new MathFunctionProvider(),
                new TouchFunctionProvider(),
                textBlockFunctionProvider,
                new ObjectDetectorFunctionProvider()
        );

        formulaFunctions = new EnumMap<>(Functions.class);
        initFunctionMap(functionProviders, formulaFunctions);
        additionalChildren = new ArrayList<>();
    }

    public FormulaElement(ElementType type, String value, FormulaElement parent) {
        this();
        this.type = type;
        this.value = value;
        this.parent = parent;
    }

    public FormulaElement(ElementType type, String value, FormulaElement parent, FormulaElement leftChild,
                          FormulaElement rightChild) {
        this(type, value, parent);
        this.leftChild = leftChild;
        this.rightChild = rightChild;

        if (leftChild != null) {
            this.leftChild.parent = this;
        }
        if (rightChild != null) {
            this.rightChild.parent = this;
        }
    }

    public FormulaElement(ElementType type, String value, FormulaElement parent, FormulaElement leftChild,
                          FormulaElement rightChild, List<FormulaElement> additionalChildren) {
        this(type, value, parent, leftChild, rightChild);
        for (FormulaElement child : additionalChildren) {
            addAdditionalChild(child);
        }
    }

    private void initFunctionMap(List<FunctionProvider> functionProviders, Map<Functions, FormulaFunction> formulaFunctions) {
        for (FunctionProvider functionProvider : functionProviders) {
            functionProvider.addFunctionsToMap(formulaFunctions);
        }

        formulaFunctions.put(Functions.RAND, new BinaryFunction(this::interpretFunctionRand));
        formulaFunctions.put(Functions.IF_THEN_ELSE, new TernaryFunction(this::interpretFunctionIfThenElse));
    }

    public ElementType getElementType() {
        return type;
    }

    private FastTwoDManager getFastTwoDManager() {
        StageActivity activity = StageActivity.activeStageActivity.get();
        if (activity != null && activity.stageListener != null) {
            return activity.stageListener.fastTwoDManager;
        }
        return null;
    }

    public String getValue() {
        return trimTrailingCharacters(value);
    }

    public List<InternToken> getInternTokenList() {
        List<InternToken> tokens = new LinkedList<>();

        switch (type) {
            case BRACKET:
                addBracketTokens(tokens, rightChild);
                break;
            case OPERATOR:
                addOperatorTokens(tokens, value);
                break;
            case FUNCTION:
                addFunctionTokens(tokens, value, leftChild, rightChild);
                break;
            case USER_VARIABLE:
                addToken(tokens, USER_VARIABLE, value);
                break;
            case USER_LIST:
                addToken(tokens, USER_LIST, value);
                break;
            case USER_DEFINED_BRICK_INPUT:
                addToken(tokens, USER_DEFINED_BRICK_INPUT, value);
                break;
            case NUMBER:
                addToken(tokens, NUMBER, trimTrailingCharacters(value));
                break;
            case SENSOR:
                addToken(tokens, SENSOR, value);
                break;
            case STRING:
                addToken(tokens, STRING, value);
                break;
            case COLLISION_FORMULA:
                addToken(tokens, COLLISION_FORMULA, value);
                break;
        }
        return tokens;
    }

    private void addToken(List<InternToken> tokens, InternTokenType tokenType) {
        tokens.add(new InternToken(tokenType));
    }

    private void addToken(List<InternToken> tokens, InternTokenType tokenType, String value) {
        tokens.add(new InternToken(tokenType, value));
    }

    private void addBracketTokens(List<InternToken> internTokenList, FormulaElement element) {
        addToken(internTokenList, BRACKET_OPEN);
        tryAddInternTokens(internTokenList, element);
        addToken(internTokenList, BRACKET_CLOSE);
    }

    private void addOperatorTokens(List<InternToken> tokens, String value) {
        tryAddInternTokens(tokens, leftChild);
        addToken(tokens, OPERATOR, value);
        tryAddInternTokens(tokens, rightChild);
    }

    private void addFunctionTokens(List<InternToken> tokens, String value, FormulaElement leftChild, FormulaElement rightChild) {
        addToken(tokens, FUNCTION_NAME, value);
        boolean functionHasParameters = false;
        if (leftChild != null) {
            addToken(tokens, FUNCTION_PARAMETERS_BRACKET_OPEN);
            functionHasParameters = true;
            tokens.addAll(leftChild.getInternTokenList());
        }
        if (rightChild != null) {
            addToken(tokens, FUNCTION_PARAMETER_DELIMITER);
            tokens.addAll(rightChild.getInternTokenList());
        }
        for (FormulaElement child : additionalChildren) {
            if (child != null) {
                addToken(tokens, FUNCTION_PARAMETER_DELIMITER);
                tokens.addAll(child.getInternTokenList());
            }
        }
        if (functionHasParameters) {
            addToken(tokens, FUNCTION_PARAMETERS_BRACKET_CLOSE);
        }
    }

    private void tryAddInternTokens(List<InternToken> tokens, FormulaElement child) {
        if (child != null) {
            tokens.addAll(child.getInternTokenList());
        }
    }

    public FormulaElement getRoot() {
        FormulaElement root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    public void updateElementByName(String oldName, String newName, ElementType type) {
        tryUpdateElementByName(leftChild, oldName, newName, type);
        tryUpdateElementByName(rightChild, oldName, newName, type);

        for (FormulaElement child : additionalChildren) {
            tryUpdateElementByName(child, oldName, newName, type);
        }

        if (matchesTypeAndName(type, oldName)) {
            value = newName;
        }
    }

    private void tryUpdateElementByName(FormulaElement element, String oldName, String newName,
                                        ElementType type) {
        if (element != null) {
            element.updateElementByName(oldName, newName, type);
        }
    }

    public final boolean containsSpriteInCollision(String name) {
        if (containsSpriteInCollision(leftChild, name) || containsSpriteInCollision(rightChild, name)) {
            return true;
        }
        for (FormulaElement child : additionalChildren) {
            if (containsSpriteInCollision(child, name)) {
                return true;
            }
        }
        return matchesTypeAndName(ElementType.COLLISION_FORMULA, name);
    }

    private boolean containsSpriteInCollision(FormulaElement element, String name) {
        return element != null && element.containsSpriteInCollision(name);
    }

    public final void insertFlattenForAllUserLists(FormulaElement element, FormulaElement parent) {
        if (element.leftChild != null) {
            insertFlattenForAllUserLists(element.leftChild, element);
        }
        if (element.rightChild != null) {
            insertFlattenForAllUserLists(element.rightChild, element);
        }
        for (FormulaElement child : element.additionalChildren) {
            if (child != null) {
                insertFlattenForAllUserLists(child, element);
            }
        }
        if (element.type == ElementType.USER_LIST && isNotUserListFunction(parent)) {
            insertFlattenBetweenParentAndElement(parent, element);
        }
    }

    public boolean isNotUserListFunction(FormulaElement element) {
        return element == null
                || element.type != ElementType.FUNCTION
                || (!element.value.equals(Functions.CONTAINS.name())
                && !element.value.equals(Functions.NUMBER_OF_ITEMS.name())
                && !element.value.equals(Functions.LIST_ITEM.name())
                && !element.value.equals(Functions.INDEX_OF_ITEM.name())
                && !element.value.equals(Functions.FLATTEN.name())
                && !element.value.equals(Functions.CONNECT.name())
                && !element.value.equals(Functions.FIND.name()));
    }

    public void insertFlattenBetweenParentAndElement(FormulaElement parent,
                                                     FormulaElement element) {
        FormulaElement flatten = new FormulaElement(ElementType.FUNCTION,
                Functions.FLATTEN.name(), parent);
        insertElementBeforeChildInFormulaTree(parent, element, flatten);
    }

    private void insertElementBeforeChildInFormulaTree(FormulaElement parent, FormulaElement child,
                                                       FormulaElement elementToInsert) {
        if (child == null || elementToInsert == null) {
            return;
        }

        child.parent = elementToInsert;
        elementToInsert.setLeftChild(child);

        if (parent == null) {
            return;
        }

        if (parent.leftChild == child) {
            parent.leftChild = elementToInsert;
        } else if (parent.rightChild == child) {
            parent.rightChild = elementToInsert;
        } else {
            for (int i = 0; i < parent.additionalChildren.size(); i++) {
                if (parent.additionalChildren.get(i) == child) {
                    parent.additionalChildren.set(i, elementToInsert);
                }
            }
        }
    }

    private boolean matchesTypeAndName(ElementType queriedType, String name) {
        return type == queriedType && value.equals(name);
    }

    public void updateCollisionFormulaToVersion(Project currentProject) {
        tryUpdateCollisionFormulaToVersion(leftChild, currentProject);
        tryUpdateCollisionFormulaToVersion(rightChild, currentProject);
        for (FormulaElement child : additionalChildren) {
            tryUpdateCollisionFormulaToVersion(child, currentProject);
        }
        if (type == ElementType.COLLISION_FORMULA) {
            String secondSpriteName = CollisionDetection.getSecondSpriteNameFromCollisionFormulaString(value, currentProject);
            if (secondSpriteName != null) {
                value = secondSpriteName;
            }
        }
    }

    private void tryUpdateCollisionFormulaToVersion(FormulaElement element, Project currentProject) {
        if (element != null) {
            element.updateCollisionFormulaToVersion(currentProject);
        }
    }

    public Object interpretRecursive(Scope scope) {
        Object rawReturnValue = rawInterpretRecursive(scope);
        return normalizeDegeneratedDoubleValues(rawReturnValue);
    }

    private Object rawInterpretRecursive(Scope scope) {
        org.catrobat.catroid.utils.PerformanceTracker.formulaEvaluations++;

        switch (type) {
            case BRACKET:
                if (additionalChildren.size() != 0) {
                    return additionalChildren.get(additionalChildren.size() - 1).interpretRecursive(scope);
                }
                return rightChild.interpretRecursive(scope);
            case NUMBER:
                if (cachedDoubleValue != null) return cachedDoubleValue;
                try {
                    cachedDoubleValue = Double.valueOf(value);
                    return cachedDoubleValue;
                } catch (NumberFormatException e) {
                    return value;
                }
            case STRING:
                return value;
            case OPERATOR:
                return tryInterpretOperator(scope, value);
            case FUNCTION:
                return interpretFunction(value, scope);
            case SENSOR:
                ProjectManager pm1 = ProjectManager.getInstance();
                Scene currentlyEditedScene = pm1 != null ? pm1.getCurrentlyEditedScene() : null;
                Project currentProject = pm1 != null ? pm1.getCurrentProject() : null;
                return interpretSensor(scope.getSprite(), currentlyEditedScene, currentProject, value);
            case USER_VARIABLE:
                if (cachedUserVariable == null || cachedScope != scope) {
                    cachedUserVariable = UserDataWrapper.getUserVariable(value, scope);
                    cachedScope = scope;
                }
                return interpretUserVariable(cachedUserVariable);

            case USER_LIST:
                if (cachedUserList == null || cachedScope != scope) {
                    cachedUserList = UserDataWrapper.getUserList(value, scope);
                    cachedScope = scope;
                }
                return interpretUserList(cachedUserList);

            case USER_DEFINED_BRICK_INPUT:
                if (cachedBrickInput == null || cachedSequence != scope.getSequence()) {
                    cachedBrickInput = UserDataWrapper.getUserDefinedBrickInput(value, scope.getSequence());
                    cachedSequence = scope.getSequence();
                }
                return interpretUserDefinedBrickInput(cachedBrickInput);
            case COLLISION_FORMULA:
                ProjectManager pm2 = ProjectManager.getInstance();
                Scene currentlyPlayingScene = pm2 != null ? pm2.getCurrentlyPlayingScene() : null;
                StageListener stageListener = StageActivity.getActiveStageListener();
                return tryInterpretCollision(scope.getSprite().look, value, currentlyPlayingScene,
                        stageListener);
        }
        return FALSE;
    }

    @NotNull
    private Object tryInterpretOperator(Scope scope, String value) {
        Operators operator = Operators.getOperatorByValue(value);
        if (operator == null) {
            return false;
        }
        if (operator == Operators.STRING_CONCAT) {
            Object leftObj = tryInterpretElementRecursive(leftChild, scope);
            Object rightObj = tryInterpretElementRecursive(rightChild, scope);

            return formatValueForStringConcat(leftObj) + formatValueForStringConcat(rightObj);
        }
        return interpretOperator(operator, scope);
    }

    private static String formatValueForStringConcat(Object rawValue) {
        if (rawValue == null) {
            return "";
        }

        if (rawValue instanceof Double) {
            double d = (Double) rawValue;
            if (Double.isNaN(d)) {
                return "";
            }
            if (d == (long) d) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }

        String str = String.valueOf(rawValue);

        if (str.endsWith(".0")) {
            try {
                double d = Double.parseDouble(str);
                if (d == (long) d) {
                    return String.valueOf((long) d);
                }
            } catch (NumberFormatException ignored) {}
        }

        return str;
    }

    private Object interpretFunction(String name, Scope scope) {
        Functions standardFunction = Functions.getFunctionByValue(name);

        if (standardFunction != null) {
            return interpretFunction(standardFunction, scope);
        } else {
            CustomFormula customFormula = CustomFormulaManager.INSTANCE.getFormulaByUniqueName(name);
            if (customFormula != null) {
                List<Object> actualArguments = new ArrayList<>();
                if (leftChild != null) actualArguments.add(tryInterpretRecursive(leftChild, scope));
                if (rightChild != null) actualArguments.add(tryInterpretRecursive(rightChild, scope));
                for (int i = 0; i < additionalChildren.size(); i++) {
                    actualArguments.add(tryInterpretRecursive(additionalChildren.get(i), scope));
                }
                return interpretCustomLunoFunction(customFormula, actualArguments, scope);
            } else {
                return FALSE;
            }
        }
    }

    private Object interpretFunction(Functions function, Scope scope) {
        if (function == Functions.IF_THEN_ELSE) {
            Double condition = convertArgumentToDouble(tryInterpretRecursive(leftChild, scope));

            if (condition == null || condition.isNaN()) return Double.NaN;

            FormulaElement branch = (condition != 0) ? rightChild : (additionalChildren.isEmpty() ? null : additionalChildren.get(0));
            Object result = tryInterpretRecursive(branch, scope);

            if (result instanceof String) return result;
            Double resultDouble = convertArgumentToDouble(result);
            return resultDouble != null ? resultDouble : 0.0;
        }

        Object arg0 = tryInterpretRecursive(leftChild, scope);
        Object arg1 = tryInterpretRecursive(rightChild, scope);
        Object arg2 = additionalChildren.isEmpty() ? null : tryInterpretRecursive(additionalChildren.get(0), scope);
        Object arg3 = additionalChildren.size() > 1 ? tryInterpretRecursive(additionalChildren.get(1), scope) : null;

        switch (function) {
            case CLAMP:
                return interpretFunctionClamp(arg0, arg1, arg2);
            case DISTAN:
                return interpretFunctionDistan(arg0, arg1, arg2, arg3);
            case LETTER:
                return interpretFunctionLetter(arg0, arg1);
            case SUBTEXT:
                return interpretFunctionSubtext(arg0, arg1, arg2);
            case FILE: {
                String fileName = String.valueOf(arg0);
                try {
                    File file = scope.getProject().getFile(fileName);
                    return file != null && file.exists() && file.isFile() && file.canRead();
                } catch (Exception e) {
                    return false;
                }
            }
            case FILES_PATH:
                return getFileListString(ProjectManager.getInstance().getCurrentProject().getFilesDir());
            case ALL_FILES:
                return ProjectManager.getInstance().getCurrentProject().getFilesDir().getAbsolutePath();
            case LUA: {
                try {
                    if (luaGlobals == null) {
                        luaGlobals = org.luaj.vm2.lib.jse.JsePlatform.standardGlobals();
                    }
                    String luaScript = String.valueOf(arg0);

                    org.luaj.vm2.LuaValue chunk = compiledLuaCache.get(luaScript);
                    if (chunk == null) {
                        chunk = luaGlobals.load(luaScript);
                        compiledLuaCache.put(luaScript, chunk);
                    }

                    return chunk.call().tojstring();
                } catch (Throwable t) {
                    android.util.Log.e("FormulaEvaluator", "Error during Lua formula execution", t);
                    return "0";
                }
            }
            case FILE_SIZE:
                return (int) getFileSize(ProjectManager.getInstance().getCurrentProject().getFile(String.valueOf(arg0)));
            case TO_HEX: {
                Integer decimal = tryParseIntFromObject(arg0);
                return decimal != null ? Integer.toHexString(decimal).toUpperCase() : "0";
            }
            case TO_DEC: {
                try {
                    return Integer.parseInt(String.valueOf(arg0), 16);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            case IS_MOUSE_BUTTON_DOWN: {
                Integer buttonCode = tryParseIntFromObject(arg0);
                if (buttonCode == null) return Conversions.FALSE;
                return Conversions.booleanToDouble(Gdx.input.isButtonPressed(buttonCode));
            }
            case RANDOM_STR: {
                String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
                Integer length = tryParseIntFromObject(arg0);
                if (length == null || length <= 0) return "";
                StringBuilder result2 = new StringBuilder(length);
                java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
                for (int i = 0; i < length; i++) {
                    result2.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
                }
                return result2.toString();
            }
            case GET_DIRECTION_X:
                return (double) com.badlogic.gdx.math.MathUtils.cosDeg((float) tryInterpretDoubleValue(arg0));
            case GET_DIRECTION_Y:
                return (double) com.badlogic.gdx.math.MathUtils.sinDeg((float) tryInterpretDoubleValue(arg0));
            case GET_ANGLE:
                return (double) com.badlogic.gdx.math.MathUtils.atan2Deg((float) tryInterpretDoubleValue(arg1), (float) tryInterpretDoubleValue(arg0));
            case GET_3D_VELOCITY_X: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getVelocity(String.valueOf(arg0)).x : 0.0;
            }
            case GET_3D_VELOCITY_Y: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getVelocity(String.valueOf(arg0)).y : 0.0;
            }
            case GET_3D_VELOCITY_Z: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getVelocity(String.valueOf(arg0)).z : 0.0;
            }
            case GET_3D_POSITION_X: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 pos = manager.getPosition(String.valueOf(arg0));
                return (pos != null) ? pos.x : 0.0;
            }
            case PT_ARGMAX:
                return MLBridge.nativeArgMax(String.valueOf(arg0));
            case F2D_IS_TOUCHED:
                return Fast2DFormulaBridge.isTouched(String.valueOf(arg0));
            case F2D_IS_TOUCHED_INDEX: {
                Integer fingerOpt = tryParseIntFromObject(arg1);
                return Fast2DFormulaBridge.isTouched(String.valueOf(arg0), fingerOpt != null ? fingerOpt : 0);
            }
            case F2D_X:
                return Fast2DFormulaBridge.getX(String.valueOf(arg0));
            case F2D_Y:
                return Fast2DFormulaBridge.getY(String.valueOf(arg0));
            case F2D_ROTATION:
                return Fast2DFormulaBridge.getRotation(String.valueOf(arg0));
            case F2D_SCALE_X:
                return Fast2DFormulaBridge.getScaleX(String.valueOf(arg0));
            case F2D_SCALE_Y:
                return Fast2DFormulaBridge.getScaleY(String.valueOf(arg0));
            case F2D_COLOR_R:
                return Fast2DFormulaBridge.getColorR(String.valueOf(arg0));
            case F2D_COLOR_G:
                return Fast2DFormulaBridge.getColorG(String.valueOf(arg0));
            case F2D_COLOR_B:
                return Fast2DFormulaBridge.getColorB(String.valueOf(arg0));
            case F2D_ALPHA:
                return Fast2DFormulaBridge.getAlpha(String.valueOf(arg0));
            case F2D_TEXTURE:
                return Fast2DFormulaBridge.getTexture(String.valueOf(arg0));
            case F2D_CAM_X:
                return Fast2DFormulaBridge.getCamX();
            case F2D_CAM_Y:
                return Fast2DFormulaBridge.getCamY();
            case F2D_CAM_ZOOM:
                return Fast2DFormulaBridge.getCamZoom();
            case PT_DUMP:
                return MLBridge.nativeGetTensorAsString(String.valueOf(arg0));
            case PT_TOTALSIZE:
                return MLBridge.nativeGetTotalSize(String.valueOf(arg0));
            case PT_VALUE: {
                Integer ptIdx = tryParseIntFromObject(arg1);
                return MLBridge.nativeGetTensorValueByIndex(String.valueOf(arg0), ptIdx != null ? ptIdx : 0);
            }
            case PT_SHAPE:
                return MLBridge.nativeGetShape(String.valueOf(arg0));
            case PT_VALUEND:
                return MLBridge.nativeGetValueND(String.valueOf(arg0), String.valueOf(arg1));
            case INTERSECT_LIST: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? manager.getIntersectionCollisionsList(String.valueOf(arg0)) : 0.0;
            }
            case COLLISION_LIST: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? manager.getPhysicsCollisionsList(String.valueOf(arg0)) : 0.0;
            }
            case DELTA: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? String.valueOf(manager.getDeltaTime()) : "-1";
            }
            case OBJECT_TOUCHES_OBJECT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? Conversions.booleanToDouble(manager.checkCollision(String.valueOf(arg0), String.valueOf(arg1))) : Conversions.FALSE;
            }
            case OBJECT_INTERSECTS_OBJECT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? Conversions.booleanToDouble(manager.checkIntersection(String.valueOf(arg0), String.valueOf(arg1))) : Conversions.FALSE;
            }
            case GET_CAMERA_ROTATION_YAW: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraRotation().y : 0.0;
            }
            case GET_CAMERA_ROTATION_PITCH: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraRotation().x : 0.0;
            }
            case GET_CAMERA_ROTATION_ROLL: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraRotation().z : 0.0;
            }
            case GET_3D_POSITION_Y: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 pos = manager.getPosition(String.valueOf(arg0));
                return (pos != null) ? pos.y : 0.0;
            }
            case GET_3D_POSITION_Z: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 pos = manager.getPosition(String.valueOf(arg0));
                return (pos != null) ? pos.z : 0.0;
            }
            case GET_CAMERA_POS_X: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraPosition().x : 0.0;
            }
            case GET_CAMERA_POS_Y: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraPosition().y : 0.0;
            }
            case GET_CAMERA_POS_Z: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraPosition().z : 0.0;
            }
            case GET_CAMERA_DIR_X: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraDirection().x : 0.0;
            }
            case GET_CAMERA_DIR_Y: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraDirection().y : 0.0;
            }
            case GET_CAMERA_DIR_Z: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraDirection().z : 0.0;
            }
            case GET_3D_ROTATION_YAW: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 rot = manager.getRotation(String.valueOf(arg0));
                return (rot != null) ? rot.y : 0.0;
            }
            case GET_3D_ROTATION_PITCH: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 rot = manager.getRotation(String.valueOf(arg0));
                return (rot != null) ? rot.x : 0.0;
            }
            case GET_3D_ROTATION_ROLL: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 rot = manager.getRotation(String.valueOf(arg0));
                return (rot != null) ? rot.z : 0.0;
            }
            case GET_3D_SCALE_X: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 scale = manager.getScale(String.valueOf(arg0));
                return (scale != null) ? scale.x : 0.0;
            }
            case GET_3D_SCALE_Y: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 scale = manager.getScale(String.valueOf(arg0));
                return (scale != null) ? scale.y : 0.0;
            }
            case GET_3D_SCALE_Z: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 scale = manager.getScale(String.valueOf(arg0));
                return (scale != null) ? scale.z : 0.0;
            }
            case GET_3D_DISTANCE: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Float dist = manager.getDistance(String.valueOf(arg0), String.valueOf(arg1));
                return (dist != null) ? dist : 0.0;
            }
            case RAY_DID_HIT2: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return FALSE;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                return booleanToDouble(result9 != null && result9.hasHit);
            }
            case RAY_HIT_SPRITE_NAME: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return "";
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit && result9.hitSprite != null) {
                    return result9.hitSprite.getName();
                }
                return "";
            }
            case RAY_HIT_X: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit) {
                    return (double) result9.hitPoint.x;
                }
                return 0.0;
            }
            case RAY_HIT_Y: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit) {
                    return (double) result9.hitPoint.y;
                }
                return 0.0;
            }
            case RAY_HIT_DISTANCE: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit) {
                    return (double) result9.hitPoint.dst(scope.getSprite().look.getX(), scope.getSprite().look.getY());
                }
                return 0.0;
            }
            case FILE_TO_BASE64: {
                String fileName = String.valueOf(arg0);
                try {
                    java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(fileName);
                    if (file != null && file.exists()) {
                        byte[] bytes = new byte[(int) file.length()];
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                            fis.read(bytes);
                        }
                        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }
            case HTTP_RESPONSE_TEXT: {
                String id = String.valueOf(arg0);
                return org.catrobat.catroid.common.ChEngineHttpManager.INSTANCE.getResponseText(id);
            }
            case HTTP_RESPONSE_CODE: {
                String id = String.valueOf(arg0);
                return (double) org.catrobat.catroid.common.ChEngineHttpManager.INSTANCE.getResponseCode(id);
            }
            case HTTP_RESPONSE_HEADER: {
                String id = String.valueOf(arg0);
                String headerName = String.valueOf(arg1);
                return org.catrobat.catroid.common.ChEngineHttpManager.INSTANCE.getResponseHeader(id, headerName);
            }
            case MQTT_MESSAGE: {
                String roomId = String.valueOf(arg0);
                return ChEngineMqttManager.INSTANCE.getLatestMessage(roomId);
            }
            case PT_SAMPLE: {
                String tensorName = String.valueOf(arg0);
                return (double) MLBridge.nativeSampleCategorical(tensorName);
            }
            case READ_FILE: {
                String filename = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(filename);
                if (file == null || !file.exists()) return "";
                try {
                    if (file.length() > 2 * 1024 * 1024) return "File is too large";
                    return com.badlogic.gdx.Gdx.files.absolute(file.getAbsolutePath()).readString("UTF-8");
                } catch (Exception e) {
                    return "";
                }
            }
            case MEDIA_DURATION: {
                String filename = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(filename);
                if (file == null || !file.exists()) return 0.0;

                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                try {
                    retriever.setDataSource(file.getAbsolutePath());
                    String timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    return Double.parseDouble(timeStr) / 1000.0;
                } catch (Exception e) {
                    return 0.0;
                } finally {
                    try { retriever.release(); } catch (Exception ignore) {}
                }
            }
            case IMAGE_WIDTH: {
                String filename = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(filename);
                if (file == null || !file.exists()) return 0.0;

                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                return (double) options.outWidth;
            }
            case IMAGE_HEIGHT: {
                String filename = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(filename);
                if (file == null || !file.exists()) return 0.0;

                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                return (double) options.outHeight;
            }
            case FILE_LAST_MODIFIED: {
                String filename = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(filename);
                if (file == null || !file.exists()) return 0.0;
                return (double) file.lastModified();
            }
            case JSON_GET:
                return interpretFunctionJsonGet(arg0, arg1);
            case JSON_SET:
                return interpretFunctionJsonSet(arg0, arg1, arg2);
            case JSON_IS_VALID:
                return interpretFunctionJsonIsValid(arg0);
            case REPEAT: {
                Integer timesOpt = tryParseIntFromObject(arg1);
                return String.valueOf(arg0).repeat(timesOpt != null ? timesOpt : 0);
            }
            case REPLACE:
                return String.valueOf(arg0).replace(String.valueOf(arg1), String.valueOf(arg2));
            case CONTAINS_STR:
                return String.valueOf(arg0).contains(String.valueOf(arg1));
            case TABLE_X:
                return TableManager.Companion.getTableXSize(String.valueOf(arg0));
            case VIEW_X: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return (double) (activity == null ? 0 : activity.getViewX(String.valueOf(arg0)));
            }
            case VIEW_Y: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return (double) (activity == null ? 0 : activity.getViewY(String.valueOf(arg0)));
            }
            case VIEW_WIDTH: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return (double) (activity == null ? 0 : activity.getViewWidth(String.valueOf(arg0)));
            }
            case VIEW_HEIGHT: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return (double) (activity == null ? 0 : activity.getViewHeight(String.valueOf(arg0)));
            }
            case RAY_DID_HIT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? Conversions.booleanToDouble(manager.getRayDidHit(String.valueOf(arg0))) : Conversions.FALSE;
            }
            case GET_RAY_HIT_X: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitPointX(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_Y: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitPointY(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_Z: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitPointZ(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_NORMAL_X: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitNormalX(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_NORMAL_Y: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitNormalY(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_NORMAL_Z: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitNormalZ(String.valueOf(arg0)) : 0.0;
            }
            case VIDEO_PLAYING: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity != null && activity.isVideoPlaying(String.valueOf(arg0));
            }
            case VIDEO_TIME: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity != null ? activity.getVideoCurrentTime(String.valueOf(arg0)) : 0;
            }
            case GET_RAY_DISTANCE: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRaycastDistance(String.valueOf(arg0)) : -1.0;
            }
            case GET_RAY_HIT_OBJECT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? manager.getRaycastHitObjectId(String.valueOf(arg0)) : "";
            }
            case CAMERA_X: {
                StageListener listener = StageActivity.getActiveStageListener();
                return listener != null ? (double) listener.getCameraPositionX() : 0.0;
            }
            case CAMERA_Y: {
                StageListener listener = StageActivity.getActiveStageListener();
                return listener != null ? (double) listener.getCameraPositionY() : 0.0;
            }
            case CAMERA_ROTATION: {
                StageListener listener = StageActivity.getActiveStageListener();
                return listener != null ? (double) listener.getCameraRotation() : 0.0;
            }
            case CAMERA_ZOOM: {
                StageListener listener = StageActivity.getActiveStageListener();
                return listener != null ? (double) listener.getCameraZoom() : 1.0;
            }
            case CHAR_TO_UNICODE: {
                try {
                    if (arg0 == null) {
                        return 0.0;
                    }
                    String str = String.valueOf(arg0);
                    if (str.isEmpty()) {
                        return 0.0;
                    }
                    return (double) str.codePointAt(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0.0;
                }
            }

            case UNICODE_TO_CHAR: {
                try {
                    if (arg0 == null) {
                        return "";
                    }
                    String valStr = String.valueOf(arg0).trim();
                    if (valStr.isEmpty()) {
                        return "";
                    }

                    int codePoint = 0;
                    String lowerStr = valStr.toLowerCase();

                    if (lowerStr.startsWith("u+") || lowerStr.startsWith("\\u")) {
                        codePoint = Integer.parseInt(valStr.substring(2), 16);
                    } else if (lowerStr.startsWith("u")) {
                        codePoint = Integer.parseInt(valStr.substring(1), 16);
                    } else if (lowerStr.startsWith("0x")) {
                        codePoint = Integer.parseInt(valStr.substring(2), 16);
                    } else {
                        try {
                            codePoint = (int) Double.parseDouble(valStr);
                        } catch (NumberFormatException nfe) {
                            codePoint = Integer.parseInt(valStr, 16);
                        }
                    }

                    if (Character.isValidCodePoint(codePoint)) {
                        return new String(Character.toChars(codePoint));
                    }
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }

            case UNESCAPE_UTF: {
                try {
                    if (arg0 == null) {
                        return "";
                    }
                    String str = String.valueOf(arg0);
                    if (str.isEmpty()) {
                        return "";
                    }

                    StringBuilder sb = new StringBuilder();
                    int i = 0;
                    int len = str.length();

                    while (i < len) {
                        char c = str.charAt(i);

                        if (c == '\\' && i + 5 < len && (str.charAt(i + 1) == 'u' || str.charAt(i + 1) == 'U')) {
                            String hex = str.substring(i + 2, i + 6);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 6;
                                continue;
                            } catch (NumberFormatException ignored) {}
                        }
                        else if ((c == 'u' || c == 'U') && i + 4 < len) {
                            boolean prevIsLetterOrDigit = (i > 0) && Character.isLetterOrDigit(str.charAt(i - 1));
                            if (!prevIsLetterOrDigit) {
                                String hex = str.substring(i + 1, i + 5);
                                try {
                                    int code = Integer.parseInt(hex, 16);
                                    sb.append((char) code);
                                    i += 5;
                                    continue;
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        sb.append(c);
                        i++;
                    }
                    return sb.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    return String.valueOf(arg0);
                }
            }
            case VOXEL_GET_ID: {
                String worldId = String.valueOf(arg0);
                int x = (int) Math.floor(tryInterpretDoubleValue(arg1));
                int y = (int) Math.floor(tryInterpretDoubleValue(arg2));
                int z = (int) Math.floor(tryInterpretDoubleValue(arg3));

                var buffer = org.catrobat.catroid.raptor.VoxelManager.Companion.getBuffer(worldId);
                return (double) (buffer != null ? buffer.get(x, y, z) : 0.0);
            }
            case VOXEL_GET_DATA: {
                String worldId = String.valueOf(arg0);
                int x = (int) Math.floor(tryInterpretDoubleValue(arg1));
                int y = (int) Math.floor(tryInterpretDoubleValue(arg2));
                int z = (int) Math.floor(tryInterpretDoubleValue(arg3));

                var buffer = org.catrobat.catroid.raptor.VoxelManager.Companion.getBuffer(worldId);
                return (double) (buffer != null ? buffer.getData(x, y, z) : 0);
            }
            case FLOATARRAY:
                return FloatArrayManager.INSTANCE.getArraySize(String.valueOf(arg0));
            case TABLE_Y:
                return TableManager.Companion.getTableYSize(String.valueOf(arg0));
            case TABLE_ELEMENT: {
                Integer tabX = tryParseIntFromObject(arg1);
                Integer tabY = tryParseIntFromObject(arg2);
                return TableManager.Companion.getElementValue(String.valueOf(arg0), tabX != null ? tabX : 0, tabY != null ? tabY : 0);
            }
            case TABLE_JOIN:
                return TableManager.Companion.tableToString(String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2));
            case UPPER:
                return String.valueOf(arg0).toUpperCase();
            case LOWER:
                return String.valueOf(arg0).toLowerCase();
            case REVERSE:
                return new StringBuilder(String.valueOf(arg0)).reverse().toString();
            case VAR:
                return UserVarsManager.INSTANCE.getVar(String.valueOf(arg0));
            case VARNAME: {
                Integer nameId = tryParseIntFromObject(arg0);
                return UserVarsManager.INSTANCE.getVarName(nameId != null ? nameId : -1);
            }
            case VARVALUE: {
                Integer valId = tryParseIntFromObject(arg0);
                return UserVarsManager.INSTANCE.getVarValue(valId != null ? valId : -1);
            }
            case LENGTH:
                return interpretFunctionLength(arg0, scope);
            case JOIN:
                return interpretFunctionJoin(scope, leftChild, rightChild);
            case JOIN3:
                return interpretFunctionJoin3(scope, leftChild, rightChild, additionalChildren);
            case MD5: {
                String text = String.valueOf(arg0);
                return org.catrobat.catroid.utils.HashUtils.hashString(text, "MD5");
            }
            case SHA256: {
                String text = String.valueOf(arg0);
                return org.catrobat.catroid.utils.HashUtils.hashString(text, "SHA-256");
            }
            case FILE_MD5: {
                String fileName = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(fileName);
                if (file != null && file.exists()) {
                    return org.catrobat.catroid.utils.HashUtils.hashFile(file, "MD5");
                }
                return "";
            }
            case FILE_SHA256: {
                String fileName = String.valueOf(arg0);
                java.io.File file = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(fileName);
                if (file != null && file.exists()) {
                    return org.catrobat.catroid.utils.HashUtils.hashFile(file, "SHA-256");
                }
                return "";
            }
            case DISTANCE:
                return interpretFunctionDistance(scope, arg0, arg1);
            case NOTIFICATION_REPLY: {
                String actionId = ChEngineNotificationManager.cleanStringId(String.valueOf(arg0));
                return ChEngineNotificationManager.INSTANCE.getSavedReplies().getOrDefault(actionId, "");
            }
            case JOINNUMBER:
                return interpretFunctionJoinNumber(scope, leftChild, rightChild);
            case REGEX:
                return tryInterpretFunctionRegex(scope, leftChild, rightChild);
            case LIST_ITEM:
                return interpretFunctionListItem(arg0, scope);
            case CONTAINS:
                return interpretFunctionContains(arg1, scope);
            case NUMBER_OF_ITEMS:
                return interpretFunctionNumberOfItems(arg0, scope);
            case INDEX_OF_ITEM:
                return interpretFunctionIndexOfItem(arg0, scope);
            case FLATTEN:
                return interpretFunctionFlatten(scope, leftChild);
            case CONNECT:
                return interpretFunctionConnect(arg1, scope);
            case FIND:
                return interpretFunctionFind(arg1, scope);
            case COLLIDES_WITH_COLOR:
                return booleanToDouble(new ColorCollisionDetection(scope, StageActivity.getActiveStageListener())
                        .tryInterpretFunctionTouchesColor(arg0));
            case COLOR_TOUCHES_COLOR:
                return booleanToDouble(new ColorCollisionDetection(scope, StageActivity.getActiveStageListener())
                        .tryInterpretFunctionColorTouchesColor(arg0, arg1));
            case COLOR_AT_XY:
                return new ColorAtXYDetection(scope, StageActivity.getActiveStageListener())
                        .tryInterpretFunctionColorAtXY(arg0, arg1);
            case TOUCHES_OBJECT_BY_NAME: {
                StageListener stageListener = StageActivity.getActiveStageListener();
                if (stageListener == null) return FALSE;
                return tryInterpretCollision(
                        scope.getSprite().look,
                        String.valueOf(arg0),
                        ProjectManager.getInstance().getCurrentlyPlayingScene(),
                        stageListener
                );
            }
            case TEXT_BLOCK_FROM_CAMERA:
                return textBlockFunctionProvider.interpretFunctionTextBlock(tryInterpretDoubleValue(arg0));
            case TEXT_BLOCK_LANGUAGE_FROM_CAMERA:
                return textBlockFunctionProvider.interpretFunctionTextBlockLanguage(tryInterpretDoubleValue(arg0));
            case COLOR_EQUALS_COLOR:
                return booleanToDouble(new ColorEqualsColor().tryInterpretFunctionColorEqualsColor(arg0, arg1, arg2));
            default:
                return interpretFormulaFunction(function, arg0, arg1, arg2);
        }
    }

    private Object interpretCustomLunoFunction(CustomFormula customFormula, List<Object> arguments, Scope scope) {
        LoadedLibrary library = LibraryManager.INSTANCE.getLoadedLibrary(customFormula.getOwnerLibraryId());
        if (library == null) {
            Log.e(TAG_FORMULA_ELEMENT, "Библиотека " + customFormula.getOwnerLibraryId() + " не загружена для функции " + customFormula.getUniqueName());
            return "LIB NOT FOUND";
        }

        Interpreter interpreter = library.getInterpreter();

        try {
            LunoValue functionValue = interpreter.getGlobals().get(new Token(TokenType.IDENTIFIER, customFormula.getLunoFunctionName(), null, -1, -1));

            if (!(functionValue instanceof LunoValue.Callable)) {
                Log.e(TAG_FORMULA_ELEMENT, "Функция " + customFormula.getLunoFunctionName() + " не найдена или не является функцией в библиотеке " + library.getId());
                return "FUNC NOT FOUND";
            }
            LunoValue.Callable lunoFunction = (LunoValue.Callable) functionValue;

            List<LunoValue> lunoArgs = new ArrayList<>();
            for (Object arg : arguments) {
                lunoArgs.add(LunoValue.Companion.fromKotlin(arg));
            }

            LunoValue result = lunoFunction.call(interpreter, lunoArgs, new Token(TokenType.EOF, "", null, -1, -1));

            Object resultJava;
            if (result instanceof LunoValue.Number) {
                resultJava = ((LunoValue.Number) result).getValue();
            } else if (result instanceof LunoValue.String) {
                resultJava = ((LunoValue.String) result).getValue();
            } else if (result instanceof LunoValue.Boolean) {
                resultJava = ((LunoValue.Boolean) result).getValue() ? Conversions.TRUE : Conversions.FALSE;
            } else {
                resultJava = interpreter.lunoValueToString(result, true);
            }

            Log.d("LunoFormulaLibs", "Функция " + customFormula.getLunoFunctionName() + " вернула LunoValue: " + result.toString() + ", конвертировано в Java: " + resultJava.toString());

            return resultJava;

        } catch (LunoRuntimeError e) {
            Log.e(TAG_FORMULA_ELEMENT, "Ошибка выполнения LunoScript для функции " + customFormula.getUniqueName(), e);
            return "LUNO ERROR";
        }
    }

    @Nullable
    private Object tryInterpretRecursive(FormulaElement element, Scope scope) {
        if (element == null) {
            return null;
        }
        return element.interpretRecursive(scope);
    }

    private Object interpretFunctionIfThenElseObject(Double condition, Object thenValue, Object elseValue) {
        if (Double.isNaN(condition)) return Double.NaN;
        if (condition != 0) return thenValue;
        return elseValue;
    }

    private double interpretFunctionIfThenElse(double condition, double thenValue, double elseValue) {
        if (Double.isNaN(condition)) return Double.NaN;
        if (condition != 0) return thenValue;
        return elseValue;
    }

    private Object interpretFormulaFunction(Functions function, Object arg0, Object arg1, Object arg2) {
        FormulaFunction formulaFunction = formulaFunctions.get(function);
        if (formulaFunction == null) {
            return FALSE;
        }

        Double val0 = convertArgumentToDouble(arg0);
        Double val1 = convertArgumentToDouble(arg1);

        double d0 = (val0 != null) ? val0 : 0.0;
        double d1 = (val1 != null) ? val1 : 0.0;

        if (arg2 == null) {
            return formulaFunction.execute(d0, d1);
        } else {
            Double val2 = convertArgumentToDouble(arg2);
            double d2 = (val2 != null) ? val2 : 0.0;
            return formulaFunction.execute(d0, d1, d2);
        }
    }

    private Object interpretFunctionNumberOfItems(Object left, Scope scope) {
        if (leftChild.type == ElementType.USER_LIST) {
            UserList userList = getUserListOfChild(leftChild, scope);
            return (double) handleNumberOfItemsOfUserListParameter(userList);
        }
        return interpretFunctionLength(left, scope);
    }

    private int handleNumberOfItemsOfUserListParameter(UserList userList) {
        if (userList == null) {
            return 0;
        }
        return userList.getValue().size();
    }

    private Object interpretFunctionContains(Object right, Scope scope) {
        UserList userList = getUserListOfChild(leftChild, scope);
        if (userList == null) {
            return FALSE;
        }
        for (Object userListElement : userList.getValue()) {
            if (interpretOperatorEqual(userListElement, right)) {
                return TRUE;
            }
        }
        return FALSE;
    }

    private Object interpretFunctionIndexOfItem(Object left, Scope scope) {
        if (rightChild.getElementType() == ElementType.USER_LIST) {
            UserList userList = getUserListOfChild(rightChild, scope);
            return (double) (userList.getIndexOf(left) + 1);
        }
        return FALSE;
    }

    private Object interpretFunctionListItem(Object left, Scope scope) {
        if (left == null) return "";
        UserList userList = getUserListOfChild(rightChild, scope);
        if (userList == null) return "";

        Integer idxOpt = tryParseIntFromObject(left);
        if (idxOpt == null) return "";

        int index = idxOpt - 1;
        if (index < 0 || index >= userList.getValue().size()) return "";
        return userList.getValue().get(index);
    }

    private Object interpretFunctionConnect(Object right, Scope scope) {
        UserList userlist = getUserListOfChild(leftChild, scope);
        if (userlist == null) return "";

        List<Object> list = userlist.getValue();
        StringBuilder strBuilder = new StringBuilder();
        String comma = String.valueOf(right);

        for (Object object : list) {
            if (object != null) {
                strBuilder.append(String.valueOf(object)).append(comma);
            }
        }
        return strBuilder.toString();
    }

    private Object interpretFunctionFind(Object right, Scope scope) {
        UserList userlist = getUserListOfChild(leftChild, scope);
        if (userlist == null) return null;

        List<Object> list = userlist.getValue();
        for (int i = 0; i < list.size(); i++) {
            Object object = list.get(i);
            if (object != null && String.valueOf(object).equals(String.valueOf(right))) {
                return i + 1;
            }
        }
        return "";
    }

    @Nullable
    private UserList getUserListOfChild(FormulaElement child, Scope scope) {
        if (child.getElementType() != ElementType.USER_LIST) {
            return null;
        }
        return UserDataWrapper.getUserList(child.value, scope);
    }

    private static String interpretFunctionJoin(Scope scope, FormulaElement leftChild, FormulaElement rightChild) {
        return interpretFunctionString(leftChild, scope).concat(interpretFunctionString(rightChild, scope));
    }

    private static String interpretFunctionJoinNumber(Scope scope, FormulaElement leftChild, FormulaElement rightChild) {
        String numb1 = interpretFunctionString(leftChild, scope);
        String numb2 = interpretFunctionString(rightChild, scope);
        return numb1 + numb2;
    }

    private static String interpretFunctionJoin3(Scope scope, FormulaElement leftChild, FormulaElement rightChild, List<FormulaElement> additionalChildren) {
        return interpretFunctionString(leftChild, scope).concat(interpretFunctionString(rightChild,
                scope).concat(interpretFunctionString(additionalChildren.get(0), scope)));
    }

    private static String interpretFunctionFlatten(Scope scope, FormulaElement leftChild) {
        return interpretFunctionString(leftChild, scope);
    }

    private static String tryInterpretFunctionRegex(Scope scope, FormulaElement leftChild, FormulaElement rightChild) {
        try {
            String left = interpretFunctionString(leftChild, scope);
            String right = interpretFunctionString(rightChild, scope);
            return interpretFunctionRegex(left, right);
        } catch (IllegalArgumentException exception) {
            return exception.getLocalizedMessage();
        }
    }

    private static String interpretFunctionRegex(String patternString, String matcherString) {
        Pattern pattern = compiledRegexCache.get(patternString);
        if (pattern == null) {
            pattern = Pattern.compile(patternString, Pattern.DOTALL | Pattern.MULTILINE);
            if (compiledRegexCache.size() < MAX_CACHED_REGEX_PATTERNS) {
                compiledRegexCache.put(patternString, pattern);
            }
        }
        Matcher matcher = pattern.matcher(matcherString);
        if (matcher.find()) {
            int groupIndex = matcher.groupCount() == 0 ? 0 : 1;
            return matcher.group(groupIndex);
        } else {
            return "";
        }
    }

    private static String interpretFunctionString(FormulaElement child, Scope scope) {
        String parameterInterpretation = "";
        if (child != null) {
            Object objectInterpretation = child.interpretRecursive(scope);
            Project project = (scope != null) ? scope.getProject() : null;
            if (project == null && ProjectManager.getInstance() != null) {
                project = ProjectManager.getInstance().getCurrentProject();
            }

            switch (child.getElementType()) {
                case STRING:
                    parameterInterpretation = child.getValue();
                    break;
                case NUMBER:
                    if (project != null) {
                        try {
                            parameterInterpretation = project.formatValue(Double.valueOf(child.getValue()));
                        } catch (Exception e) {
                            parameterInterpretation = formatNumberString((String) objectInterpretation);
                        }
                    } else {
                        parameterInterpretation = formatNumberString((String) objectInterpretation);
                    }
                    break;
                default:
                    if (project != null) {
                        parameterInterpretation = project.formatValue(objectInterpretation);
                    } else {
                        parameterInterpretation += objectInterpretation;
                    }
                    parameterInterpretation = trimTrailingCharacters(parameterInterpretation);
            }
        }
        return parameterInterpretation;
    }

    private static String formatNumberString(String numberString) {
        double number = Double.parseDouble(numberString);
        String formattedNumberString = "";
        if (!Double.isNaN(number)) {
            formattedNumberString += isInteger(number) ? (int) number : number;
        }
        return trimTrailingCharacters(formattedNumberString);
    }

    private Object interpretFunctionLength(Object left, Scope scope) {
        if (leftChild == null) {
            return FALSE;
        }
        switch (leftChild.type) {
            case NUMBER:
            case STRING:
                return (double) leftChild.value.length();
            case USER_VARIABLE:
                UserVariable userVariable = UserDataWrapper.getUserVariable(leftChild.value, scope);
                return (double) calculateUserVariableLength(userVariable);
            case USER_LIST:
                UserList userList = UserDataWrapper.getUserList(leftChild.value, scope);
                return calculateUserListLength(userList, left, scope);
            default:
                if (left instanceof Double && ((Double) left).isNaN()) {
                    return 0d;
                }
                return (double) (String.valueOf(left)).length();
        }
    }

    private int calculateUserVariableLength(UserVariable userVariable) {
        Object userVariableValue = userVariable.getValue();
        if (userVariableValue instanceof String) {
            return String.valueOf(userVariableValue).length();
        } else {
            if (userVariableValue.toString().equals("true") || userVariableValue.toString().equals("false")) {
                return 1;
            } else if (isInteger((Double) userVariableValue)) {
                return Integer.toString(((Double) userVariableValue).intValue()).length();
            } else {
                return Double.toString(((Double) userVariableValue)).length();
            }
        }
    }

    private double calculateUserListLength(UserList userList, Object left, Scope scope) {
        if (userList == null || userList.getValue().isEmpty()) {
            return FALSE;
        }

        Object interpretedList = leftChild.interpretRecursive(scope);
        if (interpretedList instanceof Double) {
            Double interpretedListDoubleValue = (Double) interpretedList;
            if (interpretedListDoubleValue.isNaN() || interpretedListDoubleValue.isInfinite()) {
                return FALSE;
            }
            return String.valueOf(interpretedListDoubleValue.intValue()).length();
        }
        if (interpretedList instanceof String) {
            return ((String) interpretedList).length();
        }
        if (left instanceof Double && ((Double) left).isNaN()) {
            return FALSE;
        }
        return String.valueOf(left).length();
    }

    private Object interpretFunctionLetter(Object left, Object right) {
        if (left == null || right == null) return "";

        Integer idxOpt = tryParseIntFromObject(left);
        if (idxOpt == null) return "";

        int index = idxOpt - 1;
        String stringValueOfRight = String.valueOf(right);

        if (index < 0 || index >= stringValueOfRight.length()) {
            return "";
        }
        return String.valueOf(stringValueOfRight.charAt(index));
    }

    public static int levenshtain(String str1, String str2) {
        int lenStr1 = str1.length();
        int lenStr2 = str2.length();

        int[][] dp = new int[lenStr1 + 1][lenStr2 + 1];

        for (int i = 0; i <= lenStr1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= lenStr2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= lenStr1; i++) {
            for (int j = 1; j <= lenStr2; j++) {
                int cost = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[lenStr1][lenStr2];
    }

    private Object interpretFunctionDistance(Scope scope, Object val1, Object val2) {
        var value1 = val1.toString();
        var value2 = val2.toString();

        return(levenshtain(value1, value2));
    }

    private Object interpretFunctionJsonIsValid(Object arg) {
        String jsonString = String.valueOf(arg);
        try {
            parseJsonRoot(jsonString);
            return Conversions.TRUE;
        } catch (Exception e) {
            return Conversions.FALSE;
        }
    }

    private Object interpretFunctionJsonGet(Object jsonArg, Object pathArg) {
        String jsonString = String.valueOf(jsonArg);
        String path = String.valueOf(pathArg);
        try {
            Object current = parseJsonRoot(jsonString);
            List<String> tokens = parsePathTokens(path);

            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (current == null || current == org.json.JSONObject.NULL) {
                    return "";
                }

                if (current instanceof org.json.JSONObject) {
                    org.json.JSONObject obj = (org.json.JSONObject) current;
                    if (i == tokens.size() - 1) {
                        return sanitizeResult(obj.opt(token));
                    } else {
                        current = obj.opt(token);
                    }
                } else if (current instanceof org.json.JSONArray) {
                    org.json.JSONArray arr = (org.json.JSONArray) current;
                    int index;
                    try {
                        index = Integer.parseInt(token);
                    } catch (NumberFormatException e) {
                        return "";
                    }
                    if (index < 0 || index >= arr.length()) {
                        return "";
                    }
                    if (i == tokens.size() - 1) {
                        return sanitizeResult(arr.opt(index));
                    } else {
                        current = arr.opt(index);
                    }
                } else {
                    return "";
                }
            }
            return sanitizeResult(current);
        } catch (Exception e) {
            return "";
        }
    }

    private Object interpretFunctionJsonSet(Object jsonArg, Object pathArg, Object valueArg) {
        String jsonString = String.valueOf(jsonArg);
        String path = String.valueOf(pathArg);
        try {
            Object root = parseJsonRoot(jsonString);
            List<String> tokens = parsePathTokens(path);
            if (tokens.isEmpty()) {
                return jsonString;
            }

            Object current = root;
            for (int i = 0; i < tokens.size() - 1; i++) {
                String token = tokens.get(i);
                String nextToken = tokens.get(i + 1);
                boolean isNextTokenIndex = isStringInteger(nextToken);

                if (current instanceof org.json.JSONObject) {
                    org.json.JSONObject currentObj = (org.json.JSONObject) current;
                    Object next = currentObj.opt(token);
                    if (next == null || next == org.json.JSONObject.NULL) {
                        next = isNextTokenIndex ? new org.json.JSONArray() : new org.json.JSONObject();
                        currentObj.put(token, next);
                    }
                    current = next;
                } else if (current instanceof org.json.JSONArray) {
                    org.json.JSONArray arr = (org.json.JSONArray) current;
                    int index = Integer.parseInt(token);
                    while (arr.length() <= index) {
                        arr.put(org.json.JSONObject.NULL);
                    }
                    Object next = arr.opt(index);
                    if (next == null || next == org.json.JSONObject.NULL) {
                        next = isNextTokenIndex ? new org.json.JSONArray() : new org.json.JSONObject();
                        arr.put(index, next);
                    }
                    current = next;
                }
            }

            String lastToken = tokens.get(tokens.size() - 1);
            Object parsedValue = parseValue(valueArg);

            if (current instanceof org.json.JSONObject) {
                ((org.json.JSONObject) current).put(lastToken, parsedValue);
            } else if (current instanceof org.json.JSONArray) {
                org.json.JSONArray arr = (org.json.JSONArray) current;
                int index = Integer.parseInt(lastToken);
                while (arr.length() <= index) {
                    arr.put(org.json.JSONObject.NULL);
                }
                arr.put(index, parsedValue);
            }

            return root.toString();
        } catch (Exception e) {
            return jsonString;
        }
    }

    private boolean isStringInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Object parseJsonRoot(String jsonString) throws org.json.JSONException {
        String trimmed = jsonString.trim();
        if (trimmed.startsWith("[")) {
            return new org.json.JSONArray(trimmed);
        } else {
            return new org.json.JSONObject(trimmed);
        }
    }

    private List<String> parsePathTokens(String path) {
        List<String> tokens = new ArrayList<>();
        String normalized = path.replace("[", ".").replace("]", "");
        String[] parts = normalized.split("\\.");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                tokens.add(part.trim());
            }
        }
        return tokens;
    }

    private Object sanitizeResult(Object result) {
        if (result == null || result == org.json.JSONObject.NULL) {
            return "";
        }
        if (result instanceof org.json.JSONObject || result instanceof org.json.JSONArray) {
            return result.toString();
        }
        return result;
    }

    private Object parseValue(Object valueArg) {
        if (valueArg == null) return org.json.JSONObject.NULL;
        String valStr = String.valueOf(valueArg);

        if ("true".equalsIgnoreCase(valStr)) return true;
        if ("false".equalsIgnoreCase(valStr)) return false;

        try {
            String trimmed = valStr.trim();
            if (trimmed.startsWith("{")) {
                return new org.json.JSONObject(trimmed);
            } else if (trimmed.startsWith("[")) {
                return new org.json.JSONArray(trimmed);
            }
        } catch (Exception ignored) {}

        try {
            if (valStr.contains(".")) {
                return Double.parseDouble(valStr);
            } else {
                return Integer.parseInt(valStr);
            }
        } catch (NumberFormatException e) {
            return valueArg;
        }
    }

    private Object interpretFunctionSubtext(Object leftArg, Object rightArg, Object stringObj) {
        if (leftArg == null || rightArg == null) return "";

        Integer startOpt = tryParseIntFromObject(leftArg);
        Integer endOpt = tryParseIntFromObject(rightArg);

        if (startOpt == null || endOpt == null) return "";

        int start = startOpt - 1;
        int end = endOpt;
        String stringValueOfString = String.valueOf(stringObj);

        if (start < 0 || end < 0 || start > end || end > stringValueOfString.length()) {
            return "";
        }

        return stringValueOfString.substring(start, end);
    }

    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private Object interpretFunctionDistan(Object x1, Object y1, Object x2, Object y2) {
        double x1s = tryInterpretDoubleValue(x1), y1s = tryInterpretDoubleValue(y1);
        double x2s = tryInterpretDoubleValue(x2), y2s = tryInterpretDoubleValue(y2);
        return calculateDistance(x1s, y1s, x2s, y2s);
    }

    private Object interpretFunctionClamp(Object valueArg, Object leftArg, Object rightArg) {
        if (leftArg == null || rightArg == null) return "";

        Integer startOpt = tryParseIntFromObject(leftArg);
        Integer endOpt = tryParseIntFromObject(rightArg);
        Integer valueOpt = tryParseIntFromObject(valueArg);

        if(startOpt == null || endOpt == null || valueOpt == null) return "";

        int start = startOpt;
        int end = endOpt;
        int value = valueOpt;

        if(value < start) value = start;
        if(value > end) value = end;

        return value;
    }

    private double interpretFunctionRand(double from, double to) {
        double low = Math.min(from, to);
        double high = Math.max(from, to);

        if (low == high) {
            return low;
        }

        if (isInteger(low) && isInteger(high)
                && !isNumberWithDecimalPoint(leftChild) && !isNumberWithDecimalPoint(rightChild)) {
            return Math.floor(Math.random() * ((high + 1) - low)) + low;
        } else {
            return (Math.random() * (high - low)) + low;
        }
    }

    private static boolean isNumberWithDecimalPoint(FormulaElement element) {
        return element != null && element.type == ElementType.NUMBER && element.value.contains(".");
    }

    private double interpretOperator(@NotNull Operators operator, Scope scope) {
        if (leftChild != null) {
            return interpretBinaryOperator(operator, scope);
        } else {
            return interpretUnaryOperator(operator, scope);
        }
    }

    private double interpretUnaryOperator(@NotNull Operators operator, Scope scope) {
        Object rightObject = tryInterpretElementRecursive(rightChild, scope);
        double right = tryInterpretDoubleValue(rightObject);

        switch (operator) {
            case MINUS:
                return -right;
            case LOGICAL_NOT:
                return booleanToDouble(right == FALSE);
            default:
                return FALSE;
        }
    }

    private double interpretBinaryOperator(@NotNull Operators operator, Scope scope) {
        Object leftObject = tryInterpretElementRecursive(leftChild, scope);
        Object rightObject = tryInterpretElementRecursive(rightChild, scope);

        Double leftDouble = tryInterpretDoubleValue(leftObject);
        Double rightDouble = tryInterpretDoubleValue(rightObject);

        double left = (leftDouble != null) ? leftDouble : 0.0;
        double right = (rightDouble != null) ? rightDouble : 0.0;

        boolean atLeastOneIsNaN = Double.isNaN(left) || Double.isNaN(right);

        switch (operator) {
            case PLUS:
                return atLeastOneIsNaN ? Double.NaN : (left + right);
            case MINUS:
                return atLeastOneIsNaN ? Double.NaN : (left - right);
            case MULT:
                return atLeastOneIsNaN ? Double.NaN : (left * right);
            case DIVIDE:
                return (atLeastOneIsNaN || right == 0.0) ? Double.NaN : (left / right);
            case POW:
                return atLeastOneIsNaN ? Double.NaN : Math.pow(left, right);
            case EQUAL:
                return booleanToDouble(interpretOperatorEqual(leftObject, rightObject));
            case NOT_EQUAL:
                return booleanToDouble(!(interpretOperatorEqual(leftObject, rightObject)));
            case GREATER_THAN:
                return booleanToDouble(left > right);
            case GREATER_OR_EQUAL:
                return booleanToDouble(left >= right);
            case SMALLER_THAN:
                return booleanToDouble(left < right);
            case SMALLER_OR_EQUAL:
                return booleanToDouble(left <= right);
            case LOGICAL_AND:
                return booleanToDouble(left != FALSE && right != FALSE);
            case LOGICAL_OR:
                return booleanToDouble(left != FALSE || right != FALSE);
            default:
                return FALSE;
        }
    }

    public FormulaElement getParent() {
        return parent;
    }

    public void setRightChild(FormulaElement rightChild) {
        this.rightChild = rightChild;
        if (this.rightChild != null) {
            this.rightChild.parent = this;
        }
    }

    public void setLeftChild(FormulaElement leftChild) {
        this.leftChild = leftChild;
        if (this.leftChild != null) {
            this.leftChild.parent = this;
        }
    }

    public void addAdditionalChild(FormulaElement child) {
        additionalChildren.add(child);
        if (child != null) {
            child.parent = this;
        }
    }

    public void replaceElement(FormulaElement current) {
        parent = current.parent;
        leftChild = current.leftChild;
        rightChild = current.rightChild;
        for (int index = 0; index < current.additionalChildren.size(); index++) {
            if (index < additionalChildren.size()) {
                additionalChildren.set(index, current.additionalChildren.get(index));
            } else {
                additionalChildren.add(current.additionalChildren.get(index));
            }
        }
        value = current.value;
        type = current.type;

        if (leftChild != null) {
            leftChild.parent = this;
        }
        if (rightChild != null) {
            rightChild.parent = this;
        }
        for (FormulaElement child : additionalChildren) {
            if (child != null) {
                child.parent = this;
            }
        }
    }

    public void replaceElement(ElementType type, String value) {
        this.value = value;
        this.type = type;
    }

    public void replaceWithSubElement(String operator, FormulaElement rightChild) {
        FormulaElement cloneThis = new FormulaElement(ElementType.OPERATOR, operator, this.getParent(), this,
                rightChild);

        cloneThis.parent.rightChild = cloneThis;
    }

    public boolean isBoolean(Scope scope) {
        if (type == ElementType.USER_VARIABLE) {
            return isUserVariableBoolean(scope);
        } else if (type == ElementType.USER_LIST) {
            return isUserListBoolean(scope);
        } else if (type == ElementType.USER_DEFINED_BRICK_INPUT) {
            return isUserDefinedBrickInputBoolean(scope);
        } else {
            return isOtherBooleanFormulaElement();
        }
    }

    private boolean isUserVariableBoolean(Scope scope) {
        UserVariable userVariable = UserDataWrapper.getUserVariable(value, scope);
        return userVariable != null && userVariable.getValue() instanceof Boolean;
    }

    private boolean isUserListBoolean(Scope scope) {
        List<Object> listValues = UserDataWrapper.getUserList(value, scope).getValue();
        if (listValues.size() != 1) {
            return false;
        }
        return listValues.get(0) instanceof Boolean;
    }

    private boolean isUserDefinedBrickInputBoolean(Scope scope) {
        UserData userData = UserDataWrapper.getUserDefinedBrickInput(value, scope.getSequence());
        if (userData != null && userData.getValue() instanceof Formula) {
            return ((Formula) userData.getValue()).getRoot().isBoolean(scope);
        } else {
            return false;
        }
    }

    private boolean isOtherBooleanFormulaElement() {
        return (type == ElementType.FUNCTION
                && Functions.isBoolean(Functions.getFunctionByValue(value)))
                || (type == ElementType.SENSOR
                && Sensors.isBoolean(Sensors.getSensorByValue(value)))
                || (type == ElementType.OPERATOR
                && Operators.getOperatorByValue(value).isLogicalOperator)
                || type == ElementType.COLLISION_FORMULA;
    }

    public boolean containsElement(ElementType elementType) {
        if (type.equals(elementType)
                || (leftChild != null && leftChild.containsElement(elementType))
                || (rightChild != null && rightChild.containsElement(elementType))) {
            return true;
        }
        for (FormulaElement child : additionalChildren) {
            if (child != null && child.containsElement(elementType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNumber() {
        if (type == ElementType.OPERATOR) {
            Operators operator = Operators.getOperatorByValue(value);
            return (operator == Operators.MINUS) && (leftChild == null) && rightChild.isNumber();
        }
        return type == ElementType.NUMBER;
    }

    @Override
    public FormulaElement clone() {
        FormulaElement leftChildClone = tryCloneElement(leftChild);
        FormulaElement rightChildClone = tryCloneElement(rightChild);
        List<FormulaElement> additionalChildrenClones = new ArrayList<>();
        for (FormulaElement child : additionalChildren) {
            additionalChildrenClones.add(tryCloneElement(child));
        }
        String valueClone = value == null ? "" : value;
        return new FormulaElement(type, valueClone, null, leftChildClone, rightChildClone,
                additionalChildrenClones);
    }

    private FormulaElement tryCloneElement(FormulaElement element) {
        return element == null ? null : element.clone();
    }

    public void addRequiredResources(final Set<Integer> requiredResourcesSet) {
        tryAddRequiredResources(requiredResourcesSet, leftChild);
        tryAddRequiredResources(requiredResourcesSet, rightChild);

        for (FormulaElement child : additionalChildren) {
            tryAddRequiredResources(requiredResourcesSet, child);
        }

        switch (type) {
            case FUNCTION:
                addFunctionResources(requiredResourcesSet, Functions.getFunctionByValue(value));
                break;
            case SENSOR:
                addSensorsResources(requiredResourcesSet, Sensors.getSensorByValue(value));
                break;
            case COLLISION_FORMULA:
                requiredResourcesSet.add(Brick.COLLISION);
                break;
            default:
        }
    }

    private void tryAddRequiredResources(Set<Integer> resourceSet, FormulaElement element) {
        if (element != null) {
            element.addRequiredResources(resourceSet);
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getUserDataRecursive(ElementType type) {
        ArrayList<String> userDataNames = new ArrayList<>();

        if (this.type == type) {
            userDataNames.add(this.value);
        }

        if (this.leftChild != null) {
            userDataNames.addAll(leftChild.getUserDataRecursive(type));
        }

        if (this.rightChild != null) {
            userDataNames.addAll(rightChild.getUserDataRecursive(type));
        }

        for (FormulaElement child : additionalChildren) {
            if (child != null) {
                userDataNames.addAll(child.getUserDataRecursive(type));
            }
        }

        return userDataNames;
    }

    public String getFileListString(File directory) {
        if (directory == null || !directory.isDirectory()) return "";

        File[] files = directory.listFiles();
        if (files == null) return "";

        StringBuilder builder = new StringBuilder();
        for (File file : files) {
            builder.append(file.getName()).append("\n");
        }
        return builder.toString();
    }

    private org.catrobat.catroid.raptor.ThreeDManager getThreeDManager() {
        org.catrobat.catroid.stage.StageListener listener = org.catrobat.catroid.stage.StageActivity.getActiveStageListener();
        if (listener != null) {
            return listener.getThreeDManager();
        }
        return null;
    }

    public long getFileSize(File file) {
        if (file == null) {
            return 0;
        }

        if (file.exists() && file.isFile()) {
            return file.length();
        }

        return 0;
    }
}
