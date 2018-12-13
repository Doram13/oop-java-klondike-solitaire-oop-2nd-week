package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import javafx.*;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        ObservableList<Card> cards = card.getContainingPile().getCards();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK && cards.indexOf(card) == cards.size()-1) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        } else if (card.getContainingPile().getPileType() == Pile.PileType.TABLEAU && cards.indexOf(card) == cards.size()-1 && card.isFaceDown()) {
            card.flip();
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        ObservableList<Card> cards = card.getContainingPile().getCards();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        if (card.getContainingPile().getPileType() == Pile.PileType.DISCARD && cards.indexOf(card) != cards.size()-1)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;
        draggedCards.clear();

        if (card.getContainingPile().getPileType() == Pile.PileType.TABLEAU && !card.isFaceDown()) {
            for (int i = cards.indexOf(card); i < cards.size(); i++) {
                Card draggedCard = cards.get(i);
                draggedCards.add(cards.get(i));
                draggedCard.getDropShadow().setRadius(20);
                draggedCard.getDropShadow().setOffsetX(10);
                draggedCard.getDropShadow().setOffsetY(10);
                draggedCard.toFront();
                draggedCard.setTranslateX(offsetX);
                draggedCard.setTranslateY(offsetY + (10 * i));
            }
        } else if (!card.isFaceDown()){
            draggedCards.add(card);
            card.getDropShadow().setRadius(20);
            card.getDropShadow().setOffsetX(10);
            card.getDropShadow().setOffsetY(10);
            card.toFront();
            card.setTranslateX(offsetX);
            card.setTranslateY(offsetY);
        }


    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        if (pile == null) {
            pile = getValidIntersectingPile(card, foundationPiles);

        }
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };

    public boolean isGameWon() {
        int numOfFullPiles = 0;
        for(Pile foundation: foundationPiles) {
            if(foundation.numOfCards() == 13) {
                numOfFullPiles++;
            }
        }
        if(numOfFullPiles == 4) {
            return true;
        }
        return false;
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        initButtons();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        ObservableList<Card> discardedCards = discardPile.getCards();
        for (int i = discardedCards.size() - 1; i > 0; i--) {
            discardedCards.get(i).flip();
            stockPile.addCard(discardedCards.get(i));
        }
        discardPile.clear();
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        Card topCard = destPile.getTopCard();
        if(tableauPiles.contains(destPile) && destPile.isEmpty()) {
            if(card.getRank() == 13) {
                return true;
            } else {
                return false;
            }
        } else if(tableauPiles.contains(destPile)) {
            if (Card.isOppositeColor(card, topCard) && topCard.getRank() - 1 == card.getRank()) {
                return true;
            }
        } else if (foundationPiles.contains(destPile)) {
            if(destPile.isEmpty() && card.getRank() == 1) {
                return true;
            } else if (!destPile.isEmpty() && topCard.getSuit() == card.getSuit() && topCard.getRank() + 1 == card.getRank()) {
                return true;
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile)) {
                result = pile;
                return result;
            }
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
        if(isGameWon()) {
            /*
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Look, a Confirmation Dialog");
            alert.setContentText("Are you ok with this?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK
            } else {
                // ... user chose CANCEL or closed the dialog
            }
            */
            return;
        }
    }


    private void initButtons() {

        //Image restartImage = new Image("restart.png");
        Image restartImage = new Image(getClass().getResourceAsStream("/button/restart.png"));
        ImageView imageView = new ImageView(restartImage);
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);
        Button restartButton = new Button("");
        restartButton.setGraphic(imageView);
        restartButton.setPrefSize(50, 50);
        restartButton.setLayoutX(1320);
        restartButton.setLayoutY(20);
        restartButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stockPile.clear();
                discardPile.clear();
                for (Pile pile : foundationPiles) {
                    pile.clear();
                }
                for (Pile pile : tableauPiles) {
                    pile.clear();
                }
                for (Card card : deck) {
                    getChildren().remove(card);
                    if (!card.isFaceDown()) {
                        card.flip();
                    }
                }
                dealCards();
            }
        });
        getChildren().add(restartButton);


    }

    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Collections.shuffle(deck);
        List<Card> tableauCards = deck.subList(0, 28);
        List<Card> stockCards = deck.subList(28, 52);
        Iterator<Card> tableauIterator = tableauCards.iterator();
        int[] numOfCardsPerPile = {1, 2, 3, 4, 5, 6, 7};
        int[] indexOfPile = {0};
        tableauIterator.forEachRemaining(card -> {
            tableauPiles.get(indexOfPile[0]).addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
            if (tableauPiles.get(indexOfPile[0]).getCards().size() == numOfCardsPerPile[indexOfPile[0]]) {
                card.flip();
                indexOfPile[0]++;
            }
        });
        Iterator<Card> deckIterator = stockCards.iterator();
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);

        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

}
